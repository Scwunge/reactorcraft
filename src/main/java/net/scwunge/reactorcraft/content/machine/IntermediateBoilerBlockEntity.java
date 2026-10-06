package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An intermediate boiler (TileEntityIntermediateBoiler): takes a fluid in from below and, each second the reactor behind it is hot enough, heats a
 * measure of it (taking the heat out of the boiler) into a hotter fluid, which collects in an output tank and flows out of the top. Stacked
 * boilers pass their fluid and output up a hundred mB at a time.
 */
public abstract class IntermediateBoilerBlockEntity extends NuclearBoilerBlockEntity {
    protected final FluidTank output;
    private final StepTimer timer = new StepTimer(20);
    private final IFluidHandler fillIn;
    private final IFluidHandler drainOut;
    private final IFluidHandler anySide;

    protected IntermediateBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, capacity);
        output = addTank("Output", capacity, s -> false);
        fillIn = FluidAccess.fillOnly(tank);
        drainOut = FluidAccess.drainOnly(output);
        anySide = new TankView(List.of(tank), List.of(output));
    }

    /** How much fluid one heating takes, in mB. */
    protected abstract int liquidUsage();

    /** The least temperature the boiler must have to heat. */
    protected abstract int minimumTemperature();

    /** Degrees one mB of the fluid takes out of the boiler. */
    protected abstract double fluidHeatCapacity();

    protected abstract Fluid inputFluid();

    protected abstract Fluid outputFluid();

    @Override
    protected void tickServer() {
        super.tickServer();
        timer.update();
        if (timer.checkCap() && canHeat()) {
            heat();
        }
        transferFluid();
    }

    public boolean canHeat() {
        return temperature >= minimumTemperature() && tank.getFluidAmount() >= liquidUsage()
                && output.getFluidAmount() < output.getCapacity() && !tank.isEmpty() && tank.getFluid().getFluid() == inputFluid();
    }

    protected void heat() {
        int amount = liquidUsage();
        temperature -= (int) (amount * fluidHeatCapacity());
        removeLiquid(tank, amount);
        addLiquid(output, outputFluid(), amount);
    }

    /** Fluid and output move up into a boiler above, a hundred mB a time. */
    private void transferFluid() {
        if (level.getBlockEntity(worldPosition.above()) instanceof IntermediateBoilerBlockEntity above && above.getType() == getType()) {
            if (above.tank.getFluidAmount() < above.tank.getCapacity() && !tank.isEmpty()) {
                int amount = Math.min(tank.getFluidAmount(), Math.min(100, above.tank.getCapacity() - above.tank.getFluidAmount()));
                addLiquid(above.tank, fluidOf(tank), amount);
                removeLiquid(tank, amount);
            }
            if (above.output.getFluidAmount() < above.output.getCapacity() && !output.isEmpty()) {
                int amount = Math.min(output.getFluidAmount(), Math.min(100, above.output.getCapacity() - above.output.getFluidAmount()));
                addLiquid(above.output, fluidOf(output), amount);
                removeLiquid(output, amount);
            }
        }
    }

    @Override
    protected boolean isValidFluid(Fluid fluid) {
        return fluid == inputFluid();
    }

    public FluidTank outputTank() {
        return output;
    }

    /** Fluid in from below, the heated fluid out of the top. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        return side == Direction.DOWN ? fillIn : side == Direction.UP ? drainOut : null;
    }
}
