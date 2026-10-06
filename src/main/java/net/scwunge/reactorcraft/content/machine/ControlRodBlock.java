package net.scwunge.reactorcraft.content.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/** The control rod: its look changes when the rod is fully down in the core (the original's getTextureState). */
public class ControlRodBlock extends ReactorMachineBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ControlRodBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type, true, true);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }
}
