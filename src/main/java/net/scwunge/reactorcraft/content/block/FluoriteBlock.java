package net.scwunge.reactorcraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.scwunge.reactorcraft.content.material.FluoriteColor;

/**
 * A block of fluorite. A stray neutron makes it fluoresce at full brightness for a second (the original's metadata + 8).
 */
public class FluoriteBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final FluoriteColor color;

    public FluoriteBlock(FluoriteColor color, Properties properties) {
        super(properties.lightLevel(state -> state.getValue(ACTIVE) ? 15 : 0));
        this.color = color;
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    public FluoriteColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    /** What a neutron passing through does: light up, and go back to normal 20 ticks later. */
    public void activate(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, 20);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
        }
    }
}
