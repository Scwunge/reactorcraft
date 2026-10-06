package net.scwunge.reactorcraft.content.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/** The coolant cell: what it is full of shows in its look (the original's getTextureState). */
public class CoolantCellBlock extends ReactorMachineBlock {
    public static final EnumProperty<CoolantState> COOLANT = EnumProperty.create("coolant", CoolantState.class);

    public CoolantCellBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type, false, false);
        registerDefaultState(defaultBlockState().setValue(COOLANT, CoolantState.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COOLANT);
    }
}
