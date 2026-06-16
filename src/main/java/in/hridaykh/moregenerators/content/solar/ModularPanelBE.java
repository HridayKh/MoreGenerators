package in.hridaykh.moregenerators.content.solar;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;

import in.hridaykh.moregenerators.collections.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

public class ModularPanelBE extends SolarBE implements IMultiBlockEntityContainer {

	public final int MAX_WIDTH = 3;
	public VoltageSourceCoupling voltageSourceCoupling;

	protected boolean updatePrevented = false;
	private BlockPos controllerPos;
	private int width = 1;

	public ModularPanelBE(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MODULAR_PANEL_BE.get(), pos, state);
	}

	@Override
	public void buildCircuit(CircuitBuilder builder) {
		if (!isController())
			return;

		builder.setTerminalCount(2);
		FloatingNode positive = builder.terminalNode(0);
		FloatingNode negative = builder.terminalNode(1);
		this.voltageSourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, positive, negative, BASE_INTERNAL_RESISTANCE);
		this.voltageSourceCoupling.setVoltage(0);
	}

	@Override
	public void electricalTick() {
		if (!isController() || this.voltageSourceCoupling == null) {
			return;
		}
		super.electricalTick();
	}

	@Override
	public void setVoltageAndResistance(float voltage, float internalResistance) {
		if (!isController() || this.voltageSourceCoupling == null)
			return;
		// it is a circuit of `getWidth()` modules of (`getWidth()` panels in connected series)
		this.voltageSourceCoupling.setVoltage(voltage * getWidth());
		this.voltageSourceCoupling.setResistance(internalResistance);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (!isController() || this.voltageSourceCoupling == null)
			return false;
		return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
	}

	// --- IMultiBlockEntityContainer Implementation ---

	@Override
	public BlockPos getController() {
		return controllerPos == null ? getBlockPos() : controllerPos;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends BlockEntity & IMultiBlockEntityContainer> T getControllerBE() {
		if (level == null)
			return null;
		BlockEntity be = level.getBlockEntity(getController());
		if (be instanceof IMultiBlockEntityContainer)
			return (T) be;
		return null;
	}

	@Override
	public boolean isController() {
		return getController().equals(getBlockPos());
	}

	@Override
	public void setController(BlockPos pos) {
		if (this.controllerPos != null && this.controllerPos.equals(pos))
			return;
		this.controllerPos = pos;
		setChanged();
		sendData();
	}

	@Override
	public void removeController(boolean keepContents) {
		this.controllerPos = null;
		this.width = 1;
		setChanged();
		sendData();
	}

	@Override
	public BlockPos getLastKnownPos() {
		return getBlockPos();
	}

	@Override
	public void preventConnectivityUpdate() {
		this.updatePrevented = true;
	}

	@Override
	public void notifyMultiUpdated() {
		this.updatePrevented = false;
		setChanged();
		sendData();
	}

	@Override
	public Direction.Axis getMainConnectionAxis() {
		return Direction.Axis.Y;
	}

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		return 1;
	}

	@Override
	public int getMaxWidth() {
		return MAX_WIDTH;
	}

	@Override
	public int getHeight() {
		return 1;
	}

	@Override
	public void setHeight(int height) {
	}

	@Override
	public int getWidth() {
		return Math.max(width, 1);
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	// --- NBT Data & Networking Updates ---

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
		width = tag.contains("Width") ? tag.getInt("Width") : 1;
	}

	@SuppressWarnings("null")
	public void sendData() {
		if (level != null && !level.isClientSide)
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		saveAdditional(tag, registries);
		return tag;
	}

	@Override
	public @NotNull ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (controllerPos != null)
			tag.putLong("Controller", controllerPos.asLong());
		tag.putInt("Width", width);
	}

	@Override
	public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
		width = tag.contains("Width") ? tag.getInt("Width") : 1;
	}

}