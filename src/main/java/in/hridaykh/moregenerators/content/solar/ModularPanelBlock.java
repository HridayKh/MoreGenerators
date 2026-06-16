package in.hridaykh.moregenerators.content.solar;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.IBE;
import in.hridaykh.moregenerators.collections.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;

import java.util.List;

public class ModularPanelBlock extends ElectricBlock implements IAcceptConnector, IBE<ModularPanelBE>, IHaveElectricProperties {
	// private static final VoxelShape SHAPE = Shapes.or(box(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F), box(1.0F, 1.0F, 1.0F, 15.0F, 2.0F, 15.0F));

	public ModularPanelBlock(BlockBehaviour.Properties settings) {
		super(settings);
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
		if (level.isClientSide)
			return;
		if (level.getBlockEntity(pos) instanceof ModularPanelBE multiBe)
			ConnectivityHandler.formMulti(multiBe);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() == newState.getBlock())
			return;
		if (level.getBlockEntity(pos) instanceof ModularPanelBE multiBe)
			ConnectivityHandler.splitMulti(multiBe);
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public boolean canConnect(LevelReader world, BlockPos pos, BlockState state, Direction side) {
		return side != Direction.UP;
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
	public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
		// Display resistance information in tooltips
		// Resistance.series(SimpleMultiBlockEntity.BASE_INTERNAL_RESISTANCE, player, tooltip);
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