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
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

import java.util.List;

// just a simple 5v source
public class SimpleMultiBlockEntity extends ElectricBlockEntity implements IMultiBlockEntityContainer, IHaveGoggleInformation {

	public static final float BASE_INTERNAL_RESISTANCE = 1.0f;
	public static final float BASE_EMF = 5.0f;
	public final int MAX_LENGTH = 2;
	public final int MAX_WIDTH = 3;
	public VoltageSourceCoupling voltageSourceCoupling;

	protected boolean updatePrevented = false;
	private BlockPos controllerPos;
	private int width = 1;
	private int height = 1;

	public SimpleMultiBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SIMPLE_MULTIBLOCK_BE.get(), pos, state);
	}

	// --- PowerGrid Electrical Routing ---

	@Override
	public void buildCircuit(CircuitBuilder builder) {
		if (!isController()) return;
		builder.setTerminalCount(2);
		FloatingNode positive = builder.terminalNode(0);
		FloatingNode negative = builder.terminalNode(1);
		this.voltageSourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, positive, negative, BASE_INTERNAL_RESISTANCE);
		// make sure tow set voltage and resistance
		setWidth(getWidth());
		setHeight(getHeight());
		this.voltageSourceCoupling.setResistance(BASE_INTERNAL_RESISTANCE * getHeight() * getWidth() * getWidth());
		this.voltageSourceCoupling.setVoltage(BASE_EMF * getHeight() * getWidth() * getWidth());
	}

	@Override
	public void electricalTick() {
		super.electricalTick();
		if (!isController() || this.voltageSourceCoupling == null) return;

		this.voltageSourceCoupling.setVoltage(5);
	}

	// --- IMultiBlockEntityContainer Implementation ---
	@Override
	public BlockPos getController() {
		return controllerPos == null ? getBlockPos() : controllerPos;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends BlockEntity & IMultiBlockEntityContainer> T getControllerBE() {
		if (level == null) return null;
		BlockEntity be = level.getBlockEntity(getController());
		if (be instanceof IMultiBlockEntityContainer) return (T) be;
		return null;
	}

	@Override
	public boolean isController() {
		return getController().equals(getBlockPos());
	}

	@Override
	public void setController(BlockPos pos) {
		if (this.controllerPos != null && this.controllerPos.equals(pos)) return;
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
		this.voltageSourceCoupling.setResistance(BASE_INTERNAL_RESISTANCE * getHeight() * getWidth() * getWidth());
		this.voltageSourceCoupling.setVoltage(BASE_EMF * getHeight() * getWidth() * getWidth());
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
		return MAX_LENGTH;
	}

	@Override
	public int getMaxWidth() {
		return MAX_WIDTH;
	}

	@Override
	public int getHeight() {
		return Math.max(height, 1);
	}

	@Override
	public void setHeight(int height) {
		MoreGenerators.LOGGER.info("{} setHeight: {}", isController(), height);
		this.voltageSourceCoupling.setResistance(BASE_INTERNAL_RESISTANCE * height * getWidth() * getWidth());
		this.voltageSourceCoupling.setVoltage(BASE_EMF * height * getWidth() * getWidth());
		this.height = height;
	}

	@Override
	public int getWidth() {
		return Math.max(width, 1);
	}

	@Override
	public void setWidth(int width) {
		MoreGenerators.LOGGER.info("{} setWidth: {}", isController(), width);
		this.voltageSourceCoupling.setResistance(BASE_INTERNAL_RESISTANCE * getHeight() * width * width);
		this.voltageSourceCoupling.setVoltage(BASE_EMF * getHeight() * width * width);
		this.width = width;
	}

	// --- NBT Data & Networking Updates ---

	@Override
	protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
		super.loadAdditional(tag, registries);
		controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
		width = tag.contains("Width") ? tag.getInt("Width") : 1;
		height = tag.contains("Height") ? tag.getInt("Height") : 1;
	}

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
		if (controllerPos != null) {
			tag.putLong("Controller", controllerPos.asLong());
		}
		tag.putInt("Width", width);
		tag.putInt("Height", height);
	}

	@Override
	public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
		width = tag.contains("Width") ? tag.getInt("Width") : 1;
		height = tag.contains("Height") ? tag.getInt("Height") : 1;
	}

	// --- Engineer's Goggles Tooltip ---

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		SimpleMultiBlockEntity controller = getControllerBE();
		if (controller == null) return false;

		tooltip.add(Component.literal("   Electric Multiblock:"));
		tooltip.add(Component.literal(String.format(" ➔ Footprint: %dx%d", controller.getWidth(), controller.getWidth())));
		tooltip.add(Component.literal(String.format(" ➔ Structure Height: %d", controller.getHeight())));
		return true;
	}
}