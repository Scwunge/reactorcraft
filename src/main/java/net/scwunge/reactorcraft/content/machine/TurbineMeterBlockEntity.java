package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Turbine Dynamometer (TileEntityTurbineMeter): finds the first turbine in the column above it (stopping at anything that blocks
 * light) and gives a redstone signal, from 0 to 15, for how fast it is spinning against its top speed.
 */
public class TurbineMeterBlockEntity extends ReactorMachineBlockEntity {
    private static final int NONE = Integer.MIN_VALUE;

    private int turbineY = NONE;
    private boolean mapped;
    private int signal;
    private int oldSignal = -1;

    public TurbineMeterBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.TURBINE_METER.get(), pos, state, 0);
    }

    @Override
    protected void tickServer() {
        if (!mapped || level.getGameTime() % 32 == 0) {
            remap();
        }
        TurbineCoreBlockEntity turbine = turbine();
        signal = turbine != null ? 15 * turbine.omega() / turbine.maxSpeed() : 0;
        if (oldSignal != signal) {
            oldSignal = signal;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    private void remap() {
        mapped = true;
        turbineY = NONE;
        for (int y = worldPosition.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            BlockPos at = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            if (level.getBlockEntity(at) instanceof TurbineCoreBlockEntity) {
                turbineY = y;
                return;
            }
            BlockState state = level.getBlockState(at);
            if (!state.isAir() && state.getLightBlock(level, at) > 0) {
                return;
            }
        }
    }

    @Nullable
    private TurbineCoreBlockEntity turbine() {
        if (turbineY == NONE) {
            return null;
        }
        return level.getBlockEntity(new BlockPos(worldPosition.getX(), turbineY, worldPosition.getZ())) instanceof TurbineCoreBlockEntity t ? t : null;
    }

    /** The redstone signal: 15 times the turbine's speed over its top speed. */
    public int signal() {
        return signal;
    }
}
