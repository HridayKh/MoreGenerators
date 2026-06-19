package in.hridaykh.moregenerators.content.solar;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.IBE;

import in.hridaykh.moregenerators.MoreGenerators;
import in.hridaykh.moregenerators.collections.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ModularPanelBlock extends ElectricBlock implements IAcceptConnector, IBE<ModularPanelBE>, IHaveElectricProperties {
	private static final VoxelShape SHAPE = Shapes.or(box(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F), box(1.0F, 1.0F, 1.0F, 15.0F, 2.0F, 15.0F));
	private static final int[][] offsetsToCheck = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };

	public ModularPanelBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ModularPanelBE(pos, state);
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (level.isClientSide())
			return;
		if (state.getBlock() == oldState.getBlock())
			return;
		rebuildMultiblocks(level, pos, false);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() == newState.getBlock())
			return;
		if (level.getBlockEntity(pos) instanceof ModularPanelBE multiBe)
			ConnectivityHandler.splitMulti(multiBe);
		rebuildMultiblocks(level, pos, true);
		super.onRemove(state, level, pos, newState, isMoving);
	}

	private void rebuildMultiblocks(Level level, BlockPos startPos, boolean processStartPos) {
		if (level.isClientSide())
			return;

		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new LinkedList<>();
		List<ModularPanelBE> panelsToProcess = new ArrayList<>();

		queue.add(startPos);
		visited.add(startPos);
		if (processStartPos && level.getBlockEntity(startPos) instanceof ModularPanelBE startPosBe)
			panelsToProcess.add(startPosBe);

		while (!queue.isEmpty()) {
			BlockPos current = queue.poll();
			for (int[] offset : offsetsToCheck) {
				BlockPos nextPos = current.offset(offset[0], 0, offset[1]);
				if (visited.contains(nextPos) || !(level.getBlockEntity(nextPos) instanceof ModularPanelBE nextBe))
					continue;
				visited.add(nextPos);
				queue.add(nextPos);
				panelsToProcess.add(nextBe);
			}
		}

		for (ModularPanelBE panel : panelsToProcess)
			ConnectivityHandler.splitMulti(panel);

		for (ModularPanelBE panel : panelsToProcess)
			ConnectivityHandler.formMulti(panel);
	}

	@Override
	public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
		if (player == null)
			return;
		Resistance.series(ModularPanelBE.BASE_INTERNAL_RESISTANCE, player, tooltip);
	}

	@Override
	public boolean canConnect(LevelReader world, BlockPos pos, BlockState state, Direction side) {
		return side == Direction.DOWN;
	}

	@Override
	public boolean isPolarized() {
		return true;
	}

	@Override
	public boolean renderPlug() {
		return true;
	}

	@Override
	public Class<ModularPanelBE> getBlockEntityClass() {
		return ModularPanelBE.class;
	}

	@Override
	public BlockEntityType<? extends ModularPanelBE> getBlockEntityType() {
		return ModBlockEntities.MODULAR_PANEL_BE.get();
	}
}
