package net.scwunge.reactorcraft.content.machine;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

/**
 * Several of a machine's tanks as one handler, for buckets and canisters used on the block itself: fills go to the first
 * input tank that takes the fluid, drains come from the first output tank that holds it.
 */
final class TankView implements IFluidHandler {
    private final List<FluidTank> inputs;
    private final List<FluidTank> outputs;

    TankView(List<FluidTank> inputs, List<FluidTank> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    public int getTanks() {
        return inputs.size() + outputs.size();
    }

    private FluidTank at(int tank) {
        return tank < inputs.size() ? inputs.get(tank) : outputs.get(tank - inputs.size());
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return at(tank).getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return at(tank).getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank < inputs.size() && inputs.get(tank).isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        for (FluidTank tank : inputs) {
            if (tank.isFluidValid(resource) && (tank.isEmpty() || tank.getFluid().is(resource.getFluid()))) {
                return tank.fill(resource, action);
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        for (FluidTank tank : outputs) {
            if (!tank.isEmpty() && tank.getFluid().is(resource.getFluid())) {
                return tank.drain(resource, action);
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (FluidTank tank : outputs) {
            if (!tank.isEmpty()) {
                return tank.drain(maxDrain, action);
            }
        }
        return FluidStack.EMPTY;
    }
}
