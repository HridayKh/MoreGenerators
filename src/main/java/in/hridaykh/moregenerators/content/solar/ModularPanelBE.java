package in.hridaykh.moregenerators.content.solar;

import in.hridaykh.moregenerators.ModLang;
import in.hridaykh.moregenerators.collections.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

import com.simibubi.create.api.connectivity.ConnectivityHandler;

public class ModularPanelBE extends AbstractModularPanelBE {

	public final int MAX_WIDTH = 10;
	public VoltageSourceCoupling voltageSourceCoupling;

	public ModularPanelBE(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MODULAR_PANEL_BE.get(), pos, state);
	}

	@Override
	public int getMaxWidth() {
		return MAX_WIDTH;
	}

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		return 1;
	}

	@Override
	public Axis getMainConnectionAxis() {
		return Direction.Axis.Y;
	}

	@Override
	public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
		if (isController()) {
			builder.setTerminalCount(2);
			FloatingNode positive = builder.terminalNode(0);
			FloatingNode negative = builder.terminalNode(1);
			this.voltageSourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, positive, negative, BASE_INTERNAL_RESISTANCE);
			this.voltageSourceCoupling.setVoltage(0);
			rebuildMemberCircuits();
		} else {
			buildMemberCircuit(builder);
		}
	}

	private void buildMemberCircuit(IElectricEntity.CircuitBuilder builder) {
		builder.setTerminalCount(2);

		ModularPanelBE controller = getControllerBE();
		if (controller == null || controller.voltageSourceCoupling == null)
			return;

		FloatingNode localPositive = builder.terminalNode(0);
		FloatingNode localNegative = builder.terminalNode(1);

		IElectricNode controllerPositive = controller.voltageSourceCoupling.getPositive();
		IElectricNode controllerNegative = controller.voltageSourceCoupling.getNegative();

		builder.connect(0.0001f, localPositive, controllerPositive);
		builder.connect(0.0001f, localNegative, controllerNegative);
	}

	private void rebuildMemberCircuits() {
		if (level == null || !isController())
			return;
		BlockPos origin = getBlockPos();
		int w = getWidth();
		int h = getHeight();
		for (int xOffset = 0; xOffset < w; xOffset++) {
			for (int zOffset = 0; zOffset < h; zOffset++) {
				BlockPos pos = origin.offset(xOffset, 0, zOffset);
				if (pos.equals(origin))
					continue;
				ModularPanelBE member = ConnectivityHandler.partAt(getType(), level, pos);
				if (member == null)
					continue;
				member.getElectricBehaviour().rebuildCircuit(false);
			}
		}
	}

	@SuppressWarnings("null")
	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide) {
			getElectricBehaviour().rebuildCircuit(false);
			if (isController())
				rebuildMemberCircuits();
		}
	}

	@Override
	public void notifyMultiUpdated() {
		super.notifyMultiUpdated();
		getElectricBehaviour().rebuildCircuit(false);
		if (isController())
			rebuildMemberCircuits();
	}

	@SuppressWarnings("null")
	@Override
	public void electricalTick() {
		if (!isController() || this.voltageSourceCoupling == null)
			return;
		if (this.level == null || this.level.isClientSide) {
			this.voltageSourceCoupling.setVoltage(0);
			return;
		}

		int baseLight = this.level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(this.worldPosition.above()) - this.level.getSkyDarken();

		float sunAngle = this.level.getSunAngle(1.0f);
		float sunIntensity = Math.max(0.0f, baseLight * Mth.cos(sunAngle) / 15.0f);

		float voltage = PEAK_VOLTAGE * sunIntensity * getWidth();
		float maxPower = PEAK_POWER * sunIntensity * getWidth() * getWidth();

		float targetResistance = BASE_INTERNAL_RESISTANCE;
		float currentDrawn = (float) Math.abs(this.voltageSourceCoupling.getCurrent());
		float currentPower = voltage * currentDrawn;

		if (currentPower > maxPower && currentDrawn > 0) {
			float targetTotalResistance = (voltage * voltage) / maxPower;
			float estimatedExternalResistance = voltage / currentDrawn;
			targetResistance = Math.max(BASE_INTERNAL_RESISTANCE, targetTotalResistance - estimatedExternalResistance);
		}

		float lerpFactor = 0.25f;
		smoothedInternalResistance += lerpFactor * (targetResistance - smoothedInternalResistance);

		this.voltageSourceCoupling.setVoltage(voltage);
		this.voltageSourceCoupling.setResistance(smoothedInternalResistance);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (this.voltageSourceCoupling == null)
			return false;
		ModLang.builder().add(Component.nullToEmpty(getWidth() + "x" + getWidth())).forGoggles(tooltip, 1);
		if (isController())
			ModLang.builder().add(Component.nullToEmpty("CONTROLLER!")).forGoggles(tooltip, 1);
		else
			ModLang.builder().add(Component.nullToEmpty(getController().toShortString())).forGoggles(tooltip, 1);
		return true;
	}

	@Override
	public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (this.voltageSourceCoupling != null)
			this.voltageSourceCoupling.setVoltage(tag.getFloat("ModularNodeValue"));
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (this.voltageSourceCoupling != null)
			tag.putFloat("ModularNodeValue", (float) this.voltageSourceCoupling.getVoltage());
	}

	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		if (this.voltageSourceCoupling != null)
			tag.putFloat("ModularNodeValue", (float) this.voltageSourceCoupling.getVoltage());
	}
}