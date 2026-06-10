package in.hridaykh.moregenerators.content.solar;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import in.hridaykh.moregenerators.ModLang;
import in.hridaykh.moregenerators.MoreGenerators;
import in.hridaykh.moregenerators.collections.ModBlockEntities;
import in.hridaykh.moregenerators.collections.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class SolarBE extends ElectricBlockEntity implements IHaveGoggleInformation {
	public static final float PEAK_POWER = 250f;
	public static final float BASE_INTERNAL_RESISTANCE = 0.05f;
	public static final float PEAK_VOLTAGE = 20f;
	public float smoothedInternalResistance = BASE_INTERNAL_RESISTANCE;
	private boolean isAngled = false;

	public VoltageSourceCoupling voltageSourceCoupling;
	public boolean overwrite = false;

	public SolarBE(BlockPos pos, BlockState state) {
		super(state.is(ModBlocks.SOLAR_PANEL.get()) ? ModBlockEntities.SOLAR_PANEL_BE.get() : ModBlockEntities.ANGLED_SOLAR_PANEL_BE.get(), pos, state);
		isAngled = state.is(ModBlocks.ANGLED_SOLAR_PANEL.get());
	}

	public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
		builder.setTerminalCount(2);
		FloatingNode positive = builder.terminalNode(0);
		FloatingNode negative = builder.terminalNode(1);
		this.voltageSourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, positive, negative, BASE_INTERNAL_RESISTANCE);
		this.voltageSourceCoupling.setVoltage(0);
	}

	@SuppressWarnings("null")
	@Override
	public void electricalTick() {
		super.electricalTick();
		if (this.level == null || this.level.isClientSide) {
			this.voltageSourceCoupling.setVoltage(0);
			return;
		}

		int baseLight = this.level.getBrightness(LightLayer.SKY, this.worldPosition) - this.level.getSkyDarken();

		Direction direction = this.getBlockState().getBedDirection(this.level, this.worldPosition);
		float sunAngle = this.level.getSunAngle(1.0f);
		MoreGenerators.LOGGER.info("Sun Angle: " + sunAngle);
		if (isAngled) {
			sunAngle += direction == Direction.EAST ? Mth.PI / 4 : 0;
			sunAngle -= direction == Direction.WEST ? Mth.PI / 4 : 0;
			MoreGenerators.LOGGER.info("Direction: " + direction);
			MoreGenerators.LOGGER.info("Sun Angle Changed: " + sunAngle);
		}

		float sunIntensity = Math.max(0.0f, baseLight * Mth.cos(sunAngle) / 15.0f);
		float voltage = PEAK_VOLTAGE * sunIntensity;
		float maxPower = PEAK_POWER * sunIntensity;

		float currentDrawn = (float) Math.abs(this.voltageSourceCoupling.getCurrent());
		float currentPower = voltage * currentDrawn;

		float targetResistance = BASE_INTERNAL_RESISTANCE;

		if (currentPower > maxPower) {
			float targetTotalResistance = (voltage * voltage) / maxPower;
			float estimatedExternalResistance = voltage / currentDrawn;
			targetResistance = Math.max(BASE_INTERNAL_RESISTANCE, targetTotalResistance - estimatedExternalResistance);
		}

		float lerpFactor = 0.25f;
		smoothedInternalResistance = smoothedInternalResistance + lerpFactor * (targetResistance - smoothedInternalResistance);

		this.voltageSourceCoupling.setResistance(smoothedInternalResistance);
		this.voltageSourceCoupling.setVoltage(voltage);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		float current = Mth.abs((float) this.voltageSourceCoupling.getCurrent());

		// Dynamically grab the current internal resistance from the simulation
		// framework
		double dynamicResistance = this.voltageSourceCoupling.getResistance();

		// V_terminal = V_source - (I * R)
		// Note: If your grid current returns negative for generation, change the '-' to
		// '+' accordingly
		double terminalVolt = (current * dynamicResistance);

		double currentGenerated = Math.abs(current);

		// Display Voltage
		ModLang.builder().text(String.format("%.2f", terminalVolt)).add(Component.nullToEmpty(" ")).add(Unit.VOLTAGE.get()).forGoggles(tooltip, 1);

		// Display Current
		ModLang.builder().text(String.format("%.2f", currentGenerated)).add(Component.nullToEmpty(" ")).add(Unit.CURRENT.get()).forGoggles(tooltip, 1);

		return true;
	}

	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (tag.contains("Overwrite"))
			this.overwrite = tag.getBoolean("Overwrite");
		this.voltageSourceCoupling.setVoltage(tag.getFloat("NodeValue"));
	}

	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (this.overwrite)
			tag.putBoolean("Overwrite", true);
		tag.putFloat("NodeValue", (float) this.voltageSourceCoupling.getVoltage());
	}

	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		if (this.overwrite)
			tag.putBoolean("Overwrite", true);
		tag.putFloat("NodeValue", (float) this.voltageSourceCoupling.getVoltage());
	}

}
