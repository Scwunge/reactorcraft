package net.scwunge.reactorcraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.scwunge.reactorcraft.content.material.FluoriteColor;

/** Fluorite ore: glows faintly (light 6), and brightly (12) for good once a neutron has passed through it. */
public class FluoriteOreBlock extends DropExperienceBlock {
    private final FluoriteColor color;

    public FluoriteOreBlock(FluoriteColor color, Properties properties) {
        super(UniformInt.of(0, 1), properties.lightLevel(state -> state.getValue(FluoriteBlock.ACTIVE) ? 12 : 6));
        this.color = color;
        registerDefaultState(defaultBlockState().setValue(FluoriteBlock.ACTIVE, false));
    }

    public FluoriteColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FluoriteBlock.ACTIVE);
    }

    public void activate(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(FluoriteBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(FluoriteBlock.ACTIVE, true), Block.UPDATE_ALL);
        }
    }
}
