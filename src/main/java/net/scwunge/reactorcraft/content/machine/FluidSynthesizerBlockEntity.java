package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.reactorcraft.registry.ReactorItems;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.Heatable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Fluid Synthesizer (TileEntitySynthesizer): heats water with quicklime and ammonium chloride into ammonia (from 220 C),
 * or turns LiFBe fuel into preheated LiFBe fuel (from 350 C, faster the hotter it is). Heat it with a friction heater.
 * Water goes in at the sides (or from a bucket in the left slot) and the product comes out of the top and bottom.
 */
public class FluidSynthesizerBlockEntity extends ReactorMachineBlockEntity implements Heatable {
    public static final int CAPACITY = 24000;
    public static final int MAX_TEMPERATURE = 1000;
    public static final int AMMONIA_TEMPERATURE = 220;
    public static final int SLOT_BUCKET = 0;
    public static final int SLOT_A = 1;
    public static final int SLOT_B = 2;

    /** The original's FluidSynthesis enum. */
    public enum Synthesis {
        AMMONIA(() -> Fluids.WATER, () -> ReactorFluids.AMMONIA.get(), 250, 1000, AMMONIA_TEMPERATURE, 50, 0,
                ItemMatch.of().tag("c:dusts/quicklime").item(ReactorItems.QUICKLIME),
                ItemMatch.of().tag("c:dusts/ammonium").item(ReactorItems.AMMONIUM_CHLORIDE)),
        HOT_LIFBE(() -> ReactorFluids.LIFBE_FUEL.get(), () -> ReactorFluids.LIFBE_FUEL_PREHEAT.get(), 50, 50, 350, 100, 5, null, null);

        private final Supplier<Fluid> input;
        private final Supplier<Fluid> output;
        public final int fluidConsumed;
        public final int fluidProduced;
        public final int minTemperature;
        public final int baseDuration;
        private final int temperatureSpeedCurve;
        @Nullable
        private final ItemMatch itemA;
        @Nullable
        private final ItemMatch itemB;

        Synthesis(Supplier<Fluid> input, Supplier<Fluid> output, int consumed, int produced, int minTemperature, int duration,
                  int curve, @Nullable ItemMatch a, @Nullable ItemMatch b) {
            this.input = input;
            this.output = output;
            this.fluidConsumed = consumed;
            this.fluidProduced = produced;
            this.minTemperature = minTemperature;
            this.baseDuration = duration;
            this.temperatureSpeedCurve = curve;
            this.itemA = a;
            this.itemB = b;
        }

        public Fluid inputFluid() {
            return input.get();
        }

        public Fluid outputFluid() {
            return output.get();
        }

        /** Ticks a cycle takes at this temperature. */
        public int duration(int temperature) {
            return Math.max(5, baseDuration - temperatureSpeedCurve * (temperature - minTemperature) / 100);
        }

        @Nullable
        public static Synthesis byInput(Fluid fluid) {
            for (Synthesis s : values()) {
                if (s.inputFluid() == fluid) {
                    return s;
                }
            }
            return null;
        }
    }

    private final StepTimer timer = new StepTimer(1800);
    private final FluidTank water;
    private final FluidTank product;
    private final IFluidHandler pipeInput;
    private final IFluidHandler pipeOutput;
    private final IFluidHandler anySide;
    @Nullable
    private Synthesis recipe;

    public FluidSynthesizerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.FLUID_SYNTHESIZER.get(), pos, state, 3);
        water = addTank("synthwater", CAPACITY, s -> Synthesis.byInput(s.getFluid()) != null);
        product = addTank("synthout", CAPACITY, s -> false);
        pipeInput = FluidAccess.fillOnly(water);
        pipeOutput = FluidAccess.drainOnly(product);
        anySide = new TankView(List.of(water), List.of(product));
    }

    @Override
    protected void tickServer() {
        if (temperature > MAX_TEMPERATURE && MachineHeat.overheat(level, worldPosition, owner())) {
            return;
        }
        loadBucket();
        recipe = findRecipe();
        if (recipe != null) {
            timer.setCap(recipe.duration(temperature));
        }
        if (recipe != null && water.getFluidAmount() >= recipe.fluidConsumed && temperature >= recipe.minTemperature
                && canTakeIn(product, recipe.outputFluid(), recipe.fluidProduced)) {
            timer.update();
            if (timer.checkCap()) {
                make(recipe);
            }
        } else {
            timer.reset();
        }
        if (thermalStep()) {
            temperature = MachineHeat.step(level, worldPosition, temperature, MAX_TEMPERATURE, owner());
            setChanged();
        }
    }

    @Nullable
    private Synthesis findRecipe() {
        if (water.isEmpty()) {
            return null;
        }
        Synthesis s = Synthesis.byInput(water.getFluid().getFluid());
        if (s == null) {
            return null;
        }
        if (s.itemA != null && !s.itemA.matches(stack(SLOT_A))) {
            return null;
        }
        if (s.itemB != null && !s.itemB.matches(stack(SLOT_B))) {
            return null;
        }
        return s;
    }

    private void make(Synthesis s) {
        if (s.itemA != null) {
            shrink(SLOT_A, 1);
        }
        if (s.itemB != null) {
            shrink(SLOT_B, 1);
        }
        removeLiquid(water, s.fluidConsumed);
        addLiquid(product, s.outputFluid(), s.fluidProduced);
    }

    /** getWaterBuckets: a single water bucket in the left slot is emptied into the water tank, leaving the bucket. */
    private void loadBucket() {
        ItemStack bucket = stack(SLOT_BUCKET);
        if (bucket.is(Items.WATER_BUCKET) && bucket.getCount() == 1 && canTakeIn(water, Fluids.WATER, FluidType.BUCKET_VOLUME)) {
            water.fill(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            setStack(SLOT_BUCKET, new ItemStack(Items.BUCKET));
        }
    }

    // ---- Heatable ----

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature += amount;
        setChanged();
    }

    @Override
    public boolean canBeFrictionHeated() {
        return true;
    }

    @Override
    public float heatMultiplier() {
        return 1;
    }

    // ---- automation ----

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        return side.getAxis().isHorizontal() ? pipeInput : pipeOutput;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_BUCKET) {
            return stack.is(Items.WATER_BUCKET);
        }
        for (Synthesis s : Synthesis.values()) {
            ItemMatch wanted = slot == SLOT_A ? s.itemA : s.itemB;
            if (wanted != null && wanted.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    /** Only the empty bucket may be taken out. */
    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return stack(slot).is(Items.BUCKET);
    }

    @Nullable
    public Synthesis recipe() {
        return recipe;
    }

    public StepTimer timer() {
        return timer;
    }

    public FluidTank waterTank() {
        return water;
    }

    public FluidTank productTank() {
        return product;
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
        menu.addMachineSlot(SLOT_BUCKET, 35, 62);
        menu.addMachineSlot(SLOT_A, 80, 26);
        menu.addMachineSlot(SLOT_B, 80, 44);
    }

    @Override
    protected int guiValueCount() {
        return 3;
    }

    @Override
    protected int getGuiValue(int index) {
        return switch (index) {
            case 0 -> temperature;
            case 1 -> timer.getTick();
            default -> timer.getCap();
        };
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Timer", timer.getTick());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        timer.setTick(tag.getInt("Timer"));
    }
}
