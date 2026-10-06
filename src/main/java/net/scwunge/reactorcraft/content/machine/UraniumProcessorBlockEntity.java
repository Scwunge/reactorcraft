package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.reactorcraft.registry.ReactorItems;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Uranium Processor (TileEntityUProcessor): turns a fluid and an item into a reactor fluid in two stages. Water and a
 * fluorite crystal make hydrofluoric acid; the acid and a uranium ingot make uranium hexafluoride (the LiFBe process
 * does the same with molten lithium and emerald dust). Fluid containers in the middle slot are emptied into the input
 * tank. Fluid comes out of the front and goes in everywhere else.
 */
public class UraniumProcessorBlockEntity extends ReactorMachineBlockEntity {
    public static final int CAPACITY = 3000;
    public static final int SLOT_FLUORITE = 0;
    public static final int SLOT_CONTAINER = 1;
    public static final int SLOT_MAIN = 2;

    /** The original's Processes enum. */
    public enum Process {
        UF6(() -> Fluids.WATER, () -> ReactorFluids.HYDROFLUORIC_ACID.get(), () -> ReactorFluids.URANIUM_HEXAFLUORIDE.get(),
                250, 1000, 250, 125, 80, 400,
                ItemMatch.of().tag("c:ingots/uranium").item(ReactorItems.URANIUM_INGOT)),
        LIFBE(() -> ReactorFluids.LITHIUM.get(), () -> ReactorFluids.HYDROFLUORIC_ACID.get(), () -> ReactorFluids.LIFBE.get(),
                100, 500, 250, 1500, 120, 600,
                ItemMatch.of().tag("c:dusts/emerald").item(ReactorItems.EMERALD_DUST));

        private final Supplier<Fluid> input;
        private final Supplier<Fluid> intermediate;
        private final Supplier<Fluid> output;
        public final int inputFluidConsumed;
        public final int outputFluidProduced;
        public final int intermediateFluidProduced;
        public final int intermediateFluidConsumed;
        public final int intermediateTime;
        public final int outputTime;
        private final ItemMatch item;

        Process(Supplier<Fluid> input, Supplier<Fluid> intermediate, Supplier<Fluid> output, int inputConsumed, int outputProduced,
                int intermediateProduced, int intermediateConsumed, int intermediateTime, int outputTime, ItemMatch item) {
            this.input = input;
            this.intermediate = intermediate;
            this.output = output;
            this.inputFluidConsumed = inputConsumed;
            this.outputFluidProduced = outputProduced;
            this.intermediateFluidProduced = intermediateProduced;
            this.intermediateFluidConsumed = intermediateConsumed;
            this.intermediateTime = intermediateTime;
            this.outputTime = outputTime;
            this.item = item;
        }

        public ItemMatch itemMatch() {
            return item;
        }

        public Fluid inputFluid() {
            return input.get();
        }

        public Fluid intermediateFluid() {
            return intermediate.get();
        }

        public Fluid outputFluid() {
            return output.get();
        }

        public boolean hasIntermediate() {
            return intermediateFluidProduced > 0;
        }

        public boolean isValidItem(ItemStack stack) {
            return item.matches(stack);
        }

        @Nullable
        public static Process byInput(Fluid fluid) {
            for (Process p : values()) {
                if (p.inputFluid() == fluid) {
                    return p;
                }
            }
            return null;
        }

        @Nullable
        public static Process byMainItem(ItemStack stack) {
            for (Process p : values()) {
                if (p.isValidItem(stack)) {
                    return p;
                }
            }
            return null;
        }
    }

    private static final ItemMatch FLUORITE = ItemMatch.of().tag("c:gems/fluorite");

    private final StepTimer intermediateTimer = new StepTimer(0);
    private final StepTimer outputTimer = new StepTimer(0);
    private final FluidTank input;
    private final FluidTank intermediate;
    private final FluidTank output;
    private final IFluidHandler pipeInput;
    private final IFluidHandler pipeOutput;
    private final IFluidHandler anySide;

    public UraniumProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.URANIUM_PROCESSOR.get(), pos, state, 3);
        input = addTank("uprocin", CAPACITY, s -> Process.byInput(s.getFluid()) != null);
        intermediate = addTank("uprocmid", CAPACITY, s -> false);
        output = addTank("uprocout", CAPACITY, s -> false);
        pipeInput = FluidAccess.fillOnly(input);
        pipeOutput = FluidAccess.drainOnly(output);
        anySide = new TankView(List.of(input), List.of(output));
    }

    @Override
    protected void tickServer() {
        loadContainer();
        Process p = process();
        if (p == null) {
            return;
        }
        intermediateTimer.setCap(p.intermediateTime);
        outputTimer.setCap(p.outputTime);
        if (p.hasIntermediate() && canRunIntermediate(p)) {
            intermediateTimer.update();
            if (intermediateTimer.checkCap()) {
                runIntermediate(p);
            }
        } else {
            intermediateTimer.reset();
        }
        if (canRunOutput(p)) {
            outputTimer.update();
            if (outputTimer.checkCap()) {
                runOutput(p);
            }
        } else {
            outputTimer.reset();
        }
    }

    @Nullable
    public Process process() {
        return input.isEmpty() ? null : Process.byInput(input.getFluid().getFluid());
    }

    public boolean canRunOutput(Process p) {
        return hasInputItem(p)
                && (!p.hasIntermediate() || intermediate.getFluidAmount() >= p.intermediateFluidConsumed)
                && canTakeIn(output, p.outputFluid(), p.outputFluidProduced);
    }

    private boolean hasInputItem(Process p) {
        return p.isValidItem(stack(SLOT_MAIN));
    }

    public boolean canRunIntermediate(Process p) {
        return !input.isEmpty()
                && canTakeIn(intermediate, p.intermediateFluid(), p.intermediateFluidProduced)
                && FLUORITE.matches(stack(SLOT_FLUORITE));
    }

    private void runIntermediate(Process p) {
        shrink(SLOT_FLUORITE, 1);
        addLiquid(intermediate, p.intermediateFluid(), p.intermediateFluidProduced);
        removeLiquid(input, p.inputFluidConsumed);
    }

    private void runOutput(Process p) {
        shrink(SLOT_MAIN, 1);
        if (!p.hasIntermediate()) {
            shrink(SLOT_FLUORITE, 1);
            removeLiquid(input, p.inputFluidConsumed);
        }
        addLiquid(output, p.outputFluid(), p.outputFluidProduced);
        removeLiquid(intermediate, p.intermediateFluidConsumed);
    }

    /** getFluidContainers: a single filled container in the middle slot is emptied into the input tank. */
    private void loadContainer() {
        ItemStack container = stack(SLOT_CONTAINER);
        if (container.isEmpty() || container.getCount() != 1) {
            return;
        }
        FluidStack contained = FluidUtil.getFluidContained(container).orElse(FluidStack.EMPTY);
        if (contained.isEmpty() || Process.byInput(contained.getFluid()) == null
                || input.fill(contained, IFluidHandler.FluidAction.SIMULATE) < contained.getAmount()) {
            return;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(container, input, contained.getAmount(), null, true);
        if (result.isSuccess()) {
            setStack(SLOT_CONTAINER, result.getResult());
        }
    }

    // ---- automation: fluid out of the front, in everywhere else ----

    private Direction facing() {
        return getBlockState().getValue(ReactorMachineBlock.LOOK);
    }

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        return side == facing() ? pipeOutput : pipeInput;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUORITE -> FLUORITE.matches(stack);
            case SLOT_CONTAINER -> FluidUtil.getFluidContained(stack).map(f -> Process.byInput(f.getFluid()) != null).orElse(false);
            case SLOT_MAIN -> Process.byMainItem(stack) != null;
            default -> false;
        };
    }

    public FluidTank inputTank() {
        return input;
    }

    public FluidTank intermediateTank() {
        return intermediate;
    }

    public FluidTank outputTank() {
        return output;
    }

    // ---- GUI ----

    @Override
    public int inventoryY() {
        return 93;
    }

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public void addMenuSlots(ReactorMenu menu) {
        menu.addMachineSlot(SLOT_FLUORITE, 44, 22);
        menu.addMachineSlot(SLOT_CONTAINER, 44, 40);
        menu.addMachineSlot(SLOT_MAIN, 44, 58);
    }

    @Override
    protected int guiValueCount() {
        return 4;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> intermediateTimer.getTick();
            case 1 -> intermediateTimer.getCap();
            case 2 -> outputTimer.getTick();
            default -> outputTimer.getCap();
        };
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IntermediateTimer", intermediateTimer.getTick());
        tag.putInt("OutputTimer", outputTimer.getTick());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        intermediateTimer.setTick(tag.getInt("IntermediateTimer"));
        outputTimer.setTick(tag.getInt("OutputTimer"));
    }
}
