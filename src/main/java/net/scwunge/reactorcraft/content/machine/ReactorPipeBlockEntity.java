package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid piping of ReactorCraft's own (TileEntityReactorPiping): it holds a thousand mB, takes a quarter of the surplus from a pipe of the same
 * kind or a machine that gives fluid out, and gives a quarter of what it has on to pipes and machines that take it. Subclasses say which fluids and which
 * neighbours it deals with.
 */
public abstract class ReactorPipeBlockEntity extends ReactorMachineBlockEntity {
    public static final int CAPACITY = 1000;

    protected final FluidTank pipe;

    protected ReactorPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        pipe = addTank("Pipe", CAPACITY, s -> isValidFluid(s.getFluid()));
    }

    public abstract boolean isValidFluid(Fluid fluid);

    /** Whether this pipe exchanges fluid with the block entity beside it. */
    protected abstract boolean isInteractable(BlockEntity other);

    protected void onIntake(BlockEntity other) {
    }

    @Override
    protected void tickServer() {
        intake();
        if (pipe.getFluidAmount() > 0) {
            dump();
        }
    }

    public FluidTank pipeTank() {
        return pipe;
    }

    /** Whether a connection is drawn to the block on {@code side}. */
    public boolean connectsTo(Direction side) {
        BlockEntity other = level.getBlockEntity(worldPosition.relative(side));
        return other != null && (other.getType() == getType() || isInteractable(other));
    }

    /** TransferAmount.FORCEDQUARTER: a quarter of the surplus, at least one while there is any. */
    private static int quarter(int surplus) {
        return surplus <= 0 ? 0 : Math.max(1, surplus / 4);
    }

    private void intake() {
        for (Direction dir : Direction.values()) {
            BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
            if (other == null || !(other.getType() == getType() || isInteractable(other))) {
                continue;
            }
            if (other instanceof ReactorPipeBlockEntity pipeOther && pipeOther.getType() == getType()) {
                FluidStack fluid = pipeOther.pipe.getFluid();
                if (!fluid.isEmpty() && canTake(fluid.getFluid())) {
                    int amount = Math.min(quarter(fluid.getAmount() - pipe.getFluidAmount()), CAPACITY - pipe.getFluidAmount());
                    if (amount > 0) {
                        addLiquid(pipe, fluid.getFluid(), amount);
                        removeLiquid(pipeOther.pipe, amount);
                        onIntake(other);
                    }
                }
                continue;
            }
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (handler == null) {
                continue;
            }
            FluidStack offered = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (!offered.isEmpty() && canTake(offered.getFluid())) {
                int amount = Math.min(quarter(offered.getAmount() - pipe.getFluidAmount()), CAPACITY - pipe.getFluidAmount());
                if (amount > 0) {
                    FluidStack drained = handler.drain(offered.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        addLiquid(pipe, drained.getFluid(), drained.getAmount());
                        onIntake(other);
                    }
                }
            }
        }
    }

    private void dump() {
        for (Direction dir : Direction.values()) {
            if (pipe.isEmpty()) {
                return;
            }
            BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
            if (other == null || !(other.getType() == getType() || isInteractable(other))) {
                continue;
            }
            Fluid fluid = pipe.getFluid().getFluid();
            if (other instanceof ReactorPipeBlockEntity pipeOther && pipeOther.getType() == getType()) {
                if (pipeOther.canTake(fluid)) {
                    int amount = Math.min(quarter(pipe.getFluidAmount() - pipeOther.pipe.getFluidAmount()), Math.max(0, pipe.getFluidAmount() - 5));
                    amount = Math.min(amount, CAPACITY - pipeOther.pipe.getFluidAmount());
                    if (amount > 0) {
                        addLiquid(pipeOther.pipe, fluid, amount);
                        removeLiquid(pipe, amount);
                    }
                }
                continue;
            }
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (handler != null) {
                int amount = Math.min(quarter(pipe.getFluidAmount()), Math.max(0, pipe.getFluidAmount() - 5));
                if (amount > 0) {
                    int added = handler.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
                    if (added > 0) {
                        removeLiquid(pipe, added);
                    }
                }
            }
        }
    }

    protected boolean canTake(Fluid fluid) {
        return isValidFluid(fluid) && (pipe.isEmpty() || pipe.getFluid().getFluid() == fluid);
    }

    /** Pipes expose no fluid handler of their own; they do the moving. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return null;
    }
}
