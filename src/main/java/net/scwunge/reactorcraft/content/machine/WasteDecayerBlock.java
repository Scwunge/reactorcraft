package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/**
 * Waste Decayer: tells whether another decayer sits above and below it, so stacked decayers look like one tall chamber
 * (the original's getTextureState: 0 alone, 1 with one above, 2 with both, 3 with one below).
 */
public class WasteDecayerBlock extends ReactorMachineBlock {
    public static final BooleanProperty ABOVE = BooleanProperty.create("above");
    public static final BooleanProperty BELOW = BooleanProperty.create("below");

    public WasteDecayerBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type, false, false);
        registerDefaultState(defaultBlockState().setValue(ABOVE, false).setValue(BELOW, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ABOVE, BELOW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state.setValue(ABOVE, context.getLevel().getBlockState(context.getClickedPos().above()).is(this))
                .setValue(BELOW, context.getLevel().getBlockState(context.getClickedPos().below()).is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.setValue(ABOVE, neighborState.is(this));
        }
        if (direction == Direction.DOWN) {
            return state.setValue(BELOW, neighborState.is(this));
        }
        return state;
    }
}
