package net.scwunge.reactorcraft.content.multi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.entity.player.Player;

/**
 * One kind of block in a multiblock structure (the original's BlockReCMultiBlock variants, one block each here). It looks different once the structure
 * it is part of is complete ({@link #FORMED}), and completing or breaking the structure is the structure's business.
 */
public class MultiPartBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final MultiStructure structure;
    private final int variant;

    public MultiPartBlock(Properties properties, MultiStructure structure, int variant) {
        super(properties);
        this.structure = structure;
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    public MultiStructure structure() {
        return structure;
    }

    public int variant() {
        return variant;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this) && !state.getValue(FORMED)) {
            structure.tryForm(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && state.getValue(FORMED)) {
            structure.breakAround(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(level, pos, state, player);
    }
}
