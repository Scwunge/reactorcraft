package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.Shockable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Electrolyzer (TileEntityElectrolyzer): splits salt into chlorine and sodium (at 801 C or more) and heavy water into
 * deuterium and oxygen. It has no shaft input; it works only while it is shocked, by a Van de Graaff generator, and
 * each discharge advances the cycle. Light products leave the top, heavy ones the bottom; water goes in at the sides.
 */
public class ElectrolyzerBlockEntity extends ReactorMachineBlockEntity implements Shockable, Heatable {
    public static final int SALT_MELT = 801;
    public static final int CAPACITY = 6000;
    public static final int MAX_TEMPERATURE = 1200;
    public static final int MIN_DISCHARGE = 4096;
    public static final int CYCLE = 50;

    /** The original's Electrolysis enum. {@code upper} goes to the top tank, {@code lower} to the bottom one. */
    public enum Electrolysis {
        SALT(null, 0, ItemMatch.of().tag("c:dusts/salt"),
                () -> ReactorFluids.CHLORINE.get(), 100, () -> ReactorFluids.SODIUM.get(), 100, SALT_MELT),
        HEAVY_WATER(() -> ReactorFluids.HEAVY_WATER.get(), 100, null,
                () -> ReactorFluids.DEUTERIUM.get(), 100, () -> ReactorFluids.OXYGEN.get(), 50, 0);

        @Nullable
        private final Supplier<Fluid> requiredFluid;
        public final int requiredFluidAmount;
        @Nullable
        private final ItemMatch requiredItem;
        private final Supplier<Fluid> upper;
        public final int upperAmount;
        private final Supplier<Fluid> lower;
        public final int lowerAmount;
        public final int requiredTemperature;

        Electrolysis(@Nullable Supplier<Fluid> in, int inAmount, @Nullable ItemMatch item, Supplier<Fluid> upper, int upperAmount,
                     Supplier<Fluid> lower, int lowerAmount, int temperature) {
            this.requiredFluid = in;
            this.requiredFluidAmount = inAmount;
            this.requiredItem = item;
            this.upper = upper;
            this.upperAmount = upperAmount;
            this.lower = lower;
            this.lowerAmount = lowerAmount;
            this.requiredTemperature = temperature;
        }

        @Nullable
        public Fluid inputFluid() {
            return requiredFluid == null ? null : requiredFluid.get();
        }

        @Nullable
        public ItemMatch inputItem() {
            return requiredItem;
        }

        public Fluid upperFluid() {
            return upper.get();
        }

        public Fluid lowerFluid() {
            return lower.get();
        }

        public boolean uses(Fluid fluid) {
            return requiredFluid != null && requiredFluid.get() == fluid;
        }

        public boolean uses(ItemStack stack) {
            return requiredItem != null && requiredItem.matches(stack);
        }
    }

    private final StepTimer timer = new StepTimer(CYCLE);
    private final FluidTank heavy;
    private final FluidTank light;
    private final FluidTank input;
    private final IFluidHandler pipeInput;
    private final IFluidHandler pipeLight;
    private final IFluidHandler pipeHeavy;
    private final IFluidHandler anySide;
    @Nullable
    private Electrolysis recipe;

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.ELECTROLYZER.get(), pos, state, 1);
        heavy = addTank("heavytank", CAPACITY, s -> false);
        light = addTank("lighttank", CAPACITY, s -> false);
        input = addTank("input", CAPACITY * 2, s -> {
            for (Electrolysis e : Electrolysis.values()) {
                if (e.uses(s.getFluid())) {
                    return true;
                }
            }
            return false;
        });
        pipeInput = FluidAccess.fillOnly(input);
        pipeLight = FluidAccess.drainOnly(light);
        pipeHeavy = FluidAccess.drainOnly(heavy);
        anySide = new TankView(List.of(input), List.of(light, heavy));
    }

    @Override
    protected void tickServer() {
        if (temperature > MAX_TEMPERATURE && MachineHeat.overheat(level, worldPosition, owner())) {
            return;
        }
        if (thermalStep()) {
            temperature = MachineHeat.step(level, worldPosition, temperature, MAX_TEMPERATURE, owner());
            setChanged();
        }
        if (recipe == null) {
            recipe = findRecipe();
        }
        if (recipe != null && requirementsMet(recipe)) {
            if (timer.checkCap()) {
                run(recipe);
            }
        } else {
            recipe = null;
            timer.reset();
        }
    }

    @Nullable
    private Electrolysis findRecipe() {
        for (Electrolysis e : Electrolysis.values()) {
            if (requirementsMet(e)) {
                return e;
            }
        }
        return null;
    }

    private boolean requirementsMet(Electrolysis e) {
        if (e.requiredFluid != null) {
            if (input.isEmpty() || !input.getFluid().is(e.requiredFluid.get()) || input.getFluidAmount() < e.requiredFluidAmount) {
                return false;
            }
        }
        if (e.requiredItem != null && !e.requiredItem.matches(stack(0))) {
            return false;
        }
        if (!canTakeIn(light, e.upperFluid(), e.upperAmount) || !canTakeIn(heavy, e.lowerFluid(), e.lowerAmount)) {
            return false;
        }
        return temperature >= e.requiredTemperature;
    }

    private void run(Electrolysis e) {
        if (e.requiredFluid != null) {
            removeLiquid(input, e.requiredFluidAmount);
        }
        if (e.requiredItem != null) {
            shrink(0, 1);
        }
        addLiquid(light, e.upperFluid(), e.upperAmount);
        addLiquid(heavy, e.lowerFluid(), e.lowerAmount);
    }

    // ---- Shockable ----

    /** Each discharge advances the cycle by sqrt(charge above the minimum) / 16 ticks, at least one. */
    @Override
    public void onDischarge(int charge, double range) {
        if (recipe != null) {
            int extra = charge - getMinDischarge();
            int n = extra > 0 ? (int) Math.sqrt(extra) / 16 : 1;
            if (n == 0) {
                n = 1;
            }
            for (int i = 0; i < n; i++) {
                timer.update();
            }
            markForSync();
        }
    }

    @Override
    public int getMinDischarge() {
        return MIN_DISCHARGE;
    }

    @Override
    public boolean canDischargeLongRange() {
        return false;
    }

    @Override
    public float getAimX() {
        return 0.5F;
    }

    @Override
    public float getAimY() {
        return 0.9375F;
    }

    @Override
    public float getAimZ() {
        return 0.5F;
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
        return 0.5F;
    }

    // ---- automation: water in at the sides, light products out of the top, heavy ones out of the bottom ----

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        return switch (side) {
            case UP -> pipeLight;
            case DOWN -> pipeHeavy;
            default -> pipeInput;
        };
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        for (Electrolysis e : Electrolysis.values()) {
            if (e.uses(stack)) {
                return true;
            }
        }
        return false;
    }

    /** The recipe being worked on, or null when idle. */
    @Nullable
    public Electrolysis recipe() {
        return recipe;
    }

    public StepTimer timer() {
        return timer;
    }

    public FluidTank inputTank() {
        return input;
    }

    public FluidTank lightTank() {
        return light;
    }

    public FluidTank heavyTank() {
        return heavy;
    }

    /** addHeavyWater: whether {@code amount} of heavy water fitted. */
    public boolean addHeavyWater(int amount) {
        if (canTakeIn(input, ReactorFluids.HEAVY_WATER.get(), amount)) {
            return input.fill(new FluidStack(ReactorFluids.HEAVY_WATER.get(), amount), IFluidHandler.FluidAction.EXECUTE) == amount;
        }
        return false;
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
        menu.addMachineSlot(0, 44, 41);
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
