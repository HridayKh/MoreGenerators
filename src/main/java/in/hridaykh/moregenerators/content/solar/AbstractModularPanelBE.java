package in.hridaykh.moregenerators.content.solar;

import org.jetbrains.annotations.NotNull;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractModularPanelBE extends SolarBE implements IMultiBlockEntityContainer {

	protected boolean updatePrevented = false;
	private BlockPos controllerPos;
	private int width = 1;
	private int height = 1;

	public AbstractModularPanelBE(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
		super(blockEntityType, pos, state);
	}

	@Override
	public abstract int getMaxLength(Direction.Axis longAxis, int width);

	@Override
	public abstract int getMaxWidth();

	@Override
	public abstract Direction.Axis getMainConnectionAxis();

	// --- IMultiBlockEntityContainer Implementation ---

	@Override
	public BlockPos getController() {
		return controllerPos == null ? getBlockPos() : controllerPos;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C extends BlockEntity & IMultiBlockEntityContainer> C getControllerBE() {
		if (level == null)
			return null;
		BlockEntity be = level.getBlockEntity(getController());
		if (be instanceof IMultiBlockEntityContainer)
			return (C) be;
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
	public int getHeight() {
		return Math.max(height, 1);
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
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
		tag.putInt("Height", height);
	}

	@Override
	public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		boolean wasController = isController();
		super.read(tag, registries, clientPacket);
		controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
		width = tag.contains("Width") ? tag.getInt("Width") : 1;
		height = tag.contains("Height") ? tag.getInt("Height") : 1;
		if (clientPacket && level != null && isController() != wasController) {
			getElectricBehaviour().rebuildCircuit(false);
		}
	}
}