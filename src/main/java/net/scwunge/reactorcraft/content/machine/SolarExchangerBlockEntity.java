package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.api.SodiumSolarUpgrades;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Solar Tower Sodium Heat Exchanger (TileEntitySolarExchanger): sits below a solar tower and, driven from below with 65536 W at 2048 rad/s or more, takes
 * hot sodium from the tower into a thousand mB tank and gives it out of its sides, for a heat exchanger. The tower itself comes from RotaryCraft.
 */
public class SolarExchangerBlockEntity extends ReactorMachineBlockEntity implements SodiumSolarUpgrades.SodiumSolarOutput {
    public static final int MIN_POWER = 65536;
    public static final int MIN_SPEED = 2048;
    public static final int CAPACITY = 1000;

    private final ShaftInput shaft = new ShaftInput();
    private final FluidTank tank;
    private final IFluidHandler drainOut;

    public SolarExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.SOLAR_EXCHANGER.get(), pos, state, 0);
        tank = addTank("Sodium", CAPACITY, s -> false);
        drainOut = FluidAccess.drainOnly(tank);
    }

    @Override
    protected void tickServer() {
        shaft.read(level, worldPosition, Direction.DOWN);
    }

    @Override
    public boolean isActive() {
        return shaft.meets(MIN_POWER, MIN_SPEED, 1);
    }

    /** The tower offers sodium: this takes what fits and returns how much of it was left. */
    @Override
    public int receiveSodium(int amount) {
        int taken = Math.max(0, Math.min(amount, CAPACITY - tank.getFluidAmount()));
        if (taken > 0) {
            addLiquid(tank, ReactorFluids.WARM_SODIUM.get(), taken);
        }
        return amount - taken;
    }

    public FluidTank sodiumTank() {
        return tank;
    }

    /** Sodium comes out of the sides only. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side.getAxis().isHorizontal() ? drainOut : null;
    }
}
