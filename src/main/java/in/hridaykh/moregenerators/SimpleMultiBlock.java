package in.hridaykh.moregenerators;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.IBE;

import in.hridaykh.moregenerators.collections.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;

public class SimpleMultiBlock extends Block implements IAcceptConnector, IBE<SimpleMultiBlockEntity> {

	public SimpleMultiBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SimpleMultiBlockEntity(pos, state);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (level.isClientSide)
			return;

		if (level.getBlockEntity(pos) instanceof SimpleMultiBlockEntity multiBe) {
			ConnectivityHandler.formMulti(multiBe);
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			if (level.getBlockEntity(pos) instanceof SimpleMultiBlockEntity multiBe) {
				ConnectivityHandler.splitMulti(multiBe);
			}
			super.onRemove(state, level, pos, newState, isMoving);
		}
	}

	@Override
	public boolean isPolarized() {
		return true;
	}

	@Override
	public boolean renderPlug() {
		return false;
	}

	@Override
	public Class<SimpleMultiBlockEntity> getBlockEntityClass() {
		return SimpleMultiBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SimpleMultiBlockEntity> getBlockEntityType() {
		return ModBlockEntities.SIMPLE_MULTIBLOCK_BE.get();
	}
}