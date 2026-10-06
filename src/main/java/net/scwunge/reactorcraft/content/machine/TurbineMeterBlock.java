package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/** The turbine dynamometer: it gives a redstone signal for how fast the turbine above it spins, and a comparator reads the same. */
public class TurbineMeterBlock extends ReactorMachineBlock {
    public TurbineMeterBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type, false, false);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TurbineMeterBlockEntity meter ? meter.signal() : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TurbineMeterBlockEntity meter ? meter.signal() : 0;
    }
}
