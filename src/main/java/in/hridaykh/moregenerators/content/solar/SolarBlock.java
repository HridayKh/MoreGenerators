package in.hridaykh.moregenerators.content.solar;

import java.util.List;

import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import com.simibubi.create.foundation.block.IBE;

import in.hridaykh.moregenerators.ModLang;
import in.hridaykh.moregenerators.collections.ModBlockEntities;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@MethodsReturnNonnullByDefault
public class SolarBlock extends ElectricBlock implements IBE<SolarBE>, IHaveElectricProperties {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = Shapes.or(box(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F), box(1.0F, 1.0F, 1.0F, 15.0F, 2.0F, 15.0F));
	private static final TerminalBoundingBox[] TERMINALS;

	static {
		var t1 = new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 10.0F, 1.0F, 0.0F, 12.0F, 3.0F, 1.0F).withColor(16726843);
		var t2 = new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 4.0F, 1.0F, 15.0F, 6.0F, 3.0F, 16.0F).withColor(3899647);
		TERMINALS = new TerminalBoundingBox[] { t1, t2 };
	}

	public SolarBlock(BlockBehaviour.Properties settings) {
		super(settings);
		VoxelShaper shaper = VoxelShaper.forHorizontal(SHAPE, Direction.NORTH);
		this.setTerminalCollection(BlockStateTerminalCollection.builder(this).forAllStates((state) -> BlockStateTerminalCollection.each(TERMINALS, (terminal) -> {
			int angle = switch (state.getValue(FACING)) {
			case NORTH -> 90;
			case SOUTH -> 270;
			case EAST -> 180;
			case WEST -> 0;
			default -> 0;
			};
			return terminal.rotateAroundY(angle);
		})).withShapeMapper((state) -> shaper.get(state.getValue(FACING))).build());
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@SuppressWarnings("null")
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction facing = ctx.getHorizontalDirection().getOpposite();
		if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown())
			facing = facing.getOpposite();
		return this.defaultBlockState().setValue(FACING, facing);
	}

	public Class<SolarBE> getBlockEntityClass() {
		return SolarBE.class;
	}

	public BlockEntityType<? extends SolarBE> getBlockEntityType() {
		return ModBlockEntities.SOLAR_PANEL_BE.get();
	}

	@Override
	public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
		ModLang.builder().add(Component.nullToEmpty("Peak Power: ")).add(Lang.number(SolarBE.PEAK_POWER)).add(Unit.POWER.get()).addTo(tooltip);
		ModLang.builder().add(Component.nullToEmpty("Internal Resistance: ")).add(Lang.number(SolarBE.BASE_INTERNAL_RESISTANCE)).add(Unit.RESISTANCE.get())
				.addTo(tooltip);
	}
}
