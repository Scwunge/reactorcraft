package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.content.block.SteamBlock;
import net.scwunge.reactorcraft.core.WorkingFluid;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Condenser (TileEntityCondenser): takes in the steam block under it, if it is cool enough (below 100 C) and has room, and
 * turns each into 200 mB of low-pressure water (or ammonia). Condensers beside each other share a quarter of their difference.
 * The liquid comes out of the top, to a pump.
 */
public class CondenserBlockEntity extends ReactorMachineBlockEntity {
    public static final int CAPACITY = 12000;
    public static final int PER_STEAM = ReactorBoilerBlockEntity.WATER_PER_STEAM;

    private final FluidTank tank;
    private final IFluidHandler output;

    public CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.CONDENSER.get(), pos, state, 0);
        tank = addTank("Tank", CAPACITY, s -> false);
        output = FluidAccess.drainOnly(tank);
    }

    @Override
    protected void tickServer() {
        BlockPos below = worldPosition.below();
        BlockState steam = level.getBlockState(below);
        if (steam.is(ReactorBlocks.STEAM.get()) && tank.getFluidAmount() + PER_STEAM <= CAPACITY && temperature < 100) {
            WorkingFluid working = steam.getValue(SteamBlock.AMMONIA) ? WorkingFluid.AMMONIA : WorkingFluid.WATER;
            Fluid fluid = working.lowPressureFluid();
            if (fluid != null && canTakeIn(tank, fluid, PER_STEAM)) {
                level.removeBlock(below, false);
                addLiquid(tank, fluid, PER_STEAM);
            }
        }
        balance();
    }

    /** A quarter of the difference flows into a condenser beside, above or below that has less. */
    private void balance() {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof CondenserBlockEntity other) {
                int dL = other.tank.getFluidAmount() - tank.getFluidAmount();
                if (dL / 4 > 0 && !other.tank.isEmpty() && canTakeIn(tank, fluidOf(other.tank), dL / 4)) {
                    addLiquid(tank, fluidOf(other.tank), dL / 4);
                    removeLiquid(other.tank, dL / 4);
                }
            }
        }
    }

    public FluidTank tank() {
        return tank;
    }

    /** The liquid comes out of the top only. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side == Direction.UP ? output : null;
    }
}
