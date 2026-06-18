package in.hridaykh.moregenerators.content.solar;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.IBE;
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

import java.util.List;

public class ModularPanelBlock extends ElectricBlock implements IAcceptConnector, IBE<ModularPanelBE>, IHaveElectricProperties {
	private static final VoxelShape SHAPE = Shapes.or(box(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F), box(1.0F, 1.0F, 1.0F, 15.0F, 2.0F, 15.0F));

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
        if (level.isClientSide) return;
        
        if (state.getBlock() == oldState.getBlock()) return;

        recalculateNetwork(level, pos, null);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() == newState.getBlock()) return;

        if (level.getBlockEntity(pos) instanceof ModularPanelBE multiBe) 
            ConnectivityHandler.splitMulti(multiBe);
        
        recalculateNetwork(level, pos, pos);

        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Performs an 8-way horizontal flood-fill search (same Y level) to discover 
     * all connected panels, dissolves them, and forces Create to reform them cleanly.
     */
    private void recalculateNetwork(Level level, BlockPos startPos, @Nullable BlockPos ignoredPos) {
        if (level.isClientSide()) return;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        List<ModularPanelBE> panelsToProcess = new ArrayList<>();

        if (ignoredPos != null) {
            visited.add(ignoredPos);
        }

        // Seed the search queue
        if (ignoredPos == null && level.getBlockEntity(startPos) instanceof ModularPanelBE startBe) {
            queue.add(startPos);
            visited.add(startPos);
            panelsToProcess.add(startBe);
        } else {
            // Check all 8 surrounding spots on the same Y-plane to seed the search
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos neighborPos = startPos.offset(x, 0, z); // Y offset hardcoded to 0
                    
                    if (neighborPos.equals(ignoredPos)) continue;
                    if (level.getBlockEntity(neighborPos) instanceof ModularPanelBE neighborBe) {
                        if (visited.add(neighborPos)) {
                            queue.add(neighborPos);
                            panelsToProcess.add(neighborBe);
                        }
                    }
                }
            }
        }

        // Run the 2D flood-fill loop
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos nextPos = current.offset(x, 0, z); // Y offset hardcoded to 0
                    
                    if (visited.contains(nextPos)) continue;

                    if (level.getBlockEntity(nextPos) instanceof ModularPanelBE nextBe) {
                        visited.add(nextPos);
                        queue.add(nextPos);
                        panelsToProcess.add(nextBe);
                    }
                }
            }
        }

        // Step 1: Break down existing structures in the network
        for (ModularPanelBE panel : panelsToProcess) {
            ConnectivityHandler.splitMulti(panel);
        }

        // Step 2: Recalculate and merge multi-blocks on the flat plane
        for (ModularPanelBE panel : panelsToProcess) {
            ConnectivityHandler.formMulti(panel);
        }
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
	public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
		Resistance.series(ModularPanelBE.BASE_INTERNAL_RESISTANCE, player, tooltip);
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
