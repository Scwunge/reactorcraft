package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.core.WorkingFluid;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reactor Pump / Pressurizer (TileEntityReactorPump): on shaft power from below (16384 W at 1024 Nm or more) it turns the
 * low-pressure water or ammonia a condenser makes back into ordinary water or ammonia, and pushes that out of its sides
 * into pipes and tanks.
 */
public class ReactorPumpBlockEntity extends ReactorMachineBlockEntity {
    public static final long MIN_POWER = 16384;
    public static final int MIN_TORQUE = 1024;
    public static final int CAPACITY = 12000;

    private final ShaftInput shaft = new ShaftInput();
    private final FluidTank input;
    private final FluidTank output;
    private final IFluidHandler pipeInput;
    private final IFluidHandler pipeOutput;
    private final IFluidHandler anySide;

    public ReactorPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.REACTOR_PUMP.get(), pos, state, 0);
        input = addTank("Input", CAPACITY, s -> isLowPressure(s.getFluid()));
        output = addTank("Output", CAPACITY, s -> false);
        pipeInput = FluidAccess.fillOnly(input);
        pipeOutput = FluidAccess.drainOnly(output);
        anySide = new TankView(List.of(input), List.of(output));
    }

    private static boolean isLowPressure(Fluid fluid) {
        return fluid == ReactorFluids.LOW_PRESSURE_WATER.get() || fluid == ReactorFluids.LOW_PRESSURE_AMMONIA.get();
    }

    @Override
    protected void tickServer() {
        int torque = shaft.torque();
        shaft.read(level, worldPosition, Direction.DOWN);
        if (torque != shaft.torque()) {
            markForSync();
        }
        if (canConvert()) {
            convert();
        }
        if (!output.isEmpty()) {
            dump();
        }
    }

    public boolean hasPower() {
        return shaft.meets(MIN_POWER, 1, MIN_TORQUE);
    }

    private boolean canConvert() {
        if (!hasPower() || input.isEmpty()) {
            return false;
        }
        if (output.isEmpty()) {
            return true;
        }
        if (output.getFluidAmount() >= output.getCapacity()) {
            return false;
        }
        Fluid in = fluidOf(input);
        return in == ReactorFluids.LOW_PRESSURE_WATER.get() ? fluidOf(output) == Fluids.WATER
                : in == ReactorFluids.LOW_PRESSURE_AMMONIA.get() && fluidOf(output) == ReactorFluids.AMMONIA.get();
    }

    private void convert() {
        int amount = Math.min(input.getFluidAmount(), output.getCapacity() - output.getFluidAmount());
        if (amount <= 0) {
            return;
        }
        Fluid in = fluidOf(input);
        if (in == ReactorFluids.LOW_PRESSURE_WATER.get()) {
            addLiquid(output, WorkingFluid.WATER.fluid(), amount);
        } else if (in == ReactorFluids.LOW_PRESSURE_AMMONIA.get()) {
            addLiquid(output, WorkingFluid.AMMONIA.fluid(), amount);
        }
        removeLiquid(input, amount);
    }

    /** Out of the four sides into whatever takes it. */
    private void dump() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (neighbour != null && !output.isEmpty()) {
                int taken = neighbour.fill(output.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                if (taken > 0) {
                    removeLiquid(output, taken);
                }
            }
        }
    }

    @Override
    protected void tickClient() {
        if (hasPower()) {
            spin(15F);
        }
    }

    /** Low-pressure fluid in from the top, pressurised fluid out of the sides. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        if (side == Direction.UP) {
            return pipeInput;
        }
        return side.getAxis().isHorizontal() ? pipeOutput : null;
    }

    public FluidTank inputTank() {
        return input;
    }

    public FluidTank outputTank() {
        return output;
    }

    public ShaftInput shaft() {
        return shaft;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        shaft.save(tag);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        shaft.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        shaft.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shaft.load(tag);
    }
}
