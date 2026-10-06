package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;
import net.scwunge.reactorcraft.core.SteamConnectable;

import java.util.function.Supplier;

/** A reactor boiler: stacked boilers look like one tall tank, and a steam line joins to the top of it. */
public class BoilerBlock extends StackableMachineBlock implements SteamConnectable {
    public BoilerBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    public boolean connectsSteam(BlockState state, Direction side) {
        return side == Direction.UP;
    }
}
