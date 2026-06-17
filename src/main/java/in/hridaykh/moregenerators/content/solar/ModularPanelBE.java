package in.hridaykh.moregenerators.content.solar;

import in.hridaykh.moregenerators.ModLang;
import in.hridaykh.moregenerators.collections.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Unit;

import com.simibubi.create.api.connectivity.ConnectivityHandler;

public class ModularPanelBE extends AbstractModularPanelBE {

	public final int MAX_WIDTH = 7;
	public VoltageSourceCoupling voltageSourceCoupling;

	public ModularPanelBE(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MODULAR_PANEL_BE.get(), pos, state);
	}

	@Override
	public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
		if (isController()) {
			builder.setTerminalCount(2);
			FloatingNode positive = builder.terminalNode(0);
			FloatingNode negative = builder.terminalNode(1);
			this.voltageSourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, positive, negative, BASE_INTERNAL_RESISTANCE);
			this.voltageSourceCoupling.setVoltage(0);

			// Tell all members to rebuild now that our coupling exists
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

		Direction.Axis axis = getMainConnectionAxis();
		BlockPos origin = getBlockPos();
		int width = getWidth();

		for (int xOffset = 0; xOffset < width; xOffset++) {
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos pos = switch (axis) {
				case X -> origin.offset(1, xOffset, zOffset);
				case Y -> origin.offset(xOffset, 1, zOffset);
				case Z -> origin.offset(xOffset, zOffset, 1);
				};

				if (pos.equals(origin))
					continue;

				ModularPanelBE member = ConnectivityHandler.partAt(getType(), level, pos);
				if (member == null)
					continue;

				member.getElectricBehaviour().rebuildCircuit(false);
			}
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
		ModLang.builder().text(String.format("%.2f", this.voltageSourceCoupling.getVoltage())).add(Component.nullToEmpty(" ")).add(Unit.VOLTAGE.get())
				.forGoggles(tooltip, 1);
		ModLang.builder().text(String.format("%.2f", Mth.abs((float) this.voltageSourceCoupling.getCurrent()))).add(Component.nullToEmpty(" "))
				.add(Unit.CURRENT.get()).forGoggles(tooltip, 1);
		ModLang.builder().add(Component.nullToEmpty(getWidth() + "s" + getWidth() + "p")).forGoggles(tooltip, 1);
		if (isController())
			ModLang.builder().add(Component.nullToEmpty("CONTROLLER!")).forGoggles(tooltip, 1);
		return true;
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
}