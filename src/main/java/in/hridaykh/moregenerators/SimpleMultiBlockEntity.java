package in.hridaykh.moregenerators;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
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
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

import java.util.List;

public class SimpleMultiBlockEntity extends ElectricBlockEntity implements IMultiBlockEntityContainer, IHaveGoggleInformation {

	private ElectricWire coil;

	private BlockPos controllerPos;
	private int width = 1;
	private int height = 1;
	protected boolean updatePrevented = false;

	public double BASE_INTERNAL_RESISTANCE = 1.0;

	public SimpleMultiBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SIMPLE_MULTIBLOCK_BE.get(), pos, state);
	}

	// --- PowerGrid Electrical Routing ---

	@Override
	public void buildCircuit(CircuitBuilder builder) {
		SimpleMultiBlockEntity controller = getControllerBE();
		if (controller == null)
			return;

		if (controller != this) {
			controller.buildCircuit(builder);
			return;
		}
		builder.setTerminalCount(2);
		this.coil = builder.connect(1.0f, builder.terminalNode(0), builder.terminalNode(1));
	}

	@Override
	public void electricalTick() {
		super.electricalTick();
		if (!isController())
			return;
		this.applyPower(this.coil);
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
		if (be instanceof IMultiBlockEntityContainer) {
			return (T) be;
		}
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
		this.height = 1;
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
		return 2;
	}

	@Override
	public int getMaxWidth() {
		return 3;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	// --- NBT Data & Networking Updates ---

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("Controller")) {
			controllerPos = BlockPos.of(tag.getLong("Controller"));
		} else {
			controllerPos = null;
		}
		width = tag.getInt("Width");
		height = tag.getInt("Height");
	}

	@SuppressWarnings("null")
	public void sendData() {
		if (level != null && !level.isClientSide)
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		saveAdditional(tag, registries);
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	// --- Engineer's Goggles Tooltip ---

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		SimpleMultiBlockEntity controller = getControllerBE();
		if (controller == null)
			return false;

		tooltip.add(Component.literal("   Electric Multiblock:"));
		tooltip.add(Component.literal(String.format(" ➔ Footprint: %dx%d", controller.getWidth(), controller.getWidth())));
		tooltip.add(Component.literal(String.format(" ➔ Structure Height: %d", controller.getHeight())));
		return true;
	}
}