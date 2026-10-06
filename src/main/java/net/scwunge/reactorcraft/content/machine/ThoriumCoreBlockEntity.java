package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.core.TemperaturedReactorTyped;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Thorium Fuel Core (TileEntityThoriumCore): a liquid-fuelled core, fed lithium beryllium fluoride fuel salt from above. It makes no
 * neutrons of its own; each neutron it catches may fission 100 mB of fuel into hot fuel salt (best between 400 and 1200 C, and
 * ever less likely as it heats: it cannot run away), leaving a little liquid waste that poisons it as it builds up. Hot fuel salt leaves from below, waste from
 * the sides; stacked and neighbouring cores share their tanks. Above 1100 C a fuel dump valve below drains its fuel away.
 */
public class ThoriumCoreBlockEntity extends NuclearCoreBlockEntity {
    public static final int CYCLE_AMOUNT = 100;
    public static final int FUEL_DUMP_TEMPERATURE = 1100;
    public static final int MIN_TEMPERATURE = 400;
    public static final int MAX_TEMPERATURE = 1200;

    private final FluidTank fuel;
    private final FluidTank hotFuel;
    private final FluidTank waste;
    private final IFluidHandler fuelIn;
    private final IFluidHandler hotOut;
    private final IFluidHandler wasteOut;
    private final IFluidHandler anySide;
    private final StepTimer balanceTimer = new StepTimer(5);

    public ThoriumCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.THORIUM_CORE.get(), pos, state);
        fuel = addTank("FuelSalts", 4000, s -> s.getFluid() == ReactorFluids.LIFBE_FUEL.get() || s.getFluid() == ReactorFluids.LIFBE_FUEL_PREHEAT.get());
        hotFuel = addTank("HotFuelSalts", 4000, s -> false);
        waste = addTank("Waste", 1000, s -> false);
        fuelIn = FluidAccess.fillOnly(fuel);
        hotOut = FluidAccess.drainOnly(hotFuel);
        wasteOut = FluidAccess.drainOnly(waste);
        anySide = new TankView(List.of(fuel), List.of(hotFuel, waste));
    }

    @Override
    protected void tickServer() {
        super.tickServer();
        balanceTimer.update();
        if (balanceTimer.checkCap()) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
                if (other instanceof ThoriumCoreBlockEntity core) {
                    balance(waste, core.waste);
                    balance(fuel, core.fuel);
                    balance(hotFuel, core.hotFuel);
                }
            }
        }
        feedFluid();
    }

    private void balance(FluidTank from, FluidTank to) {
        if (from.isEmpty() || (!to.isEmpty() && !to.getFluid().is(fluidOf(from)))) {
            return;
        }
        int dl = from.getFluidAmount() - to.getFluidAmount();
        if (dl > 1) {
            int amount = Math.min(from.getFluidAmount() / 4, Math.max(1, dl / 8 + 1));
            if (amount > 0) {
                addLiquid(to, fluidOf(from), amount);
                removeLiquid(from, amount);
            }
        }
    }

    /** feedFluid: everything in the three tanks passes on down into a thorium core below. */
    private void feedFluid() {
        if (level.getBlockEntity(worldPosition.below()) instanceof ThoriumCoreBlockEntity below) {
            pass(fuel, below.fuel);
            pass(hotFuel, below.hotFuel);
            pass(waste, below.waste);
        }
    }

    private void pass(FluidTank from, FluidTank to) {
        if (from.isEmpty() || (!to.isEmpty() && !to.getFluid().is(fluidOf(from)))) {
            return;
        }
        int amount = Math.min(to.getCapacity() - to.getFluidAmount(), from.getFluidAmount());
        if (amount > 0) {
            addLiquid(to, fluidOf(from), amount);
            removeLiquid(from, amount);
        }
    }

    /** Preheated fuel keeps the core at 250 C. */
    @Override
    protected int restingTemperature() {
        return !fuel.isEmpty() && fuel.getFluid().is(ReactorFluids.LIFBE_FUEL_PREHEAT.get()) ? 250 : super.restingTemperature();
    }

    @Override
    protected int decayNeutronChance() {
        return 30;
    }

    @Override
    protected int warningTemperature() {
        return 900;
    }

    @Override
    protected int ambientHeatLossFactor(int base, int ambient) {
        return ambient < temperature ? base * 4 : base / 2;
    }

    @Override
    protected float heatConductionThroughput(TemperaturedReactorTyped other) {
        return other.getReactorType() != ReactorType.THORIUM ? 0.25F : super.heatConductionThroughput(other);
    }

    @Override
    protected int heatConductionFraction(TemperaturedReactorTyped other) {
        return other.getReactorType() == ReactorType.FISSION ? 2 : super.heatConductionFraction(other);
    }

    @Override
    protected float heatConductionEfficiency(TemperaturedReactorTyped other) {
        boolean rest = temperature - restingTemperature() < 50;
        if (other instanceof ReactorBoilerBlockEntity) {
            return rest ? 0.125F : 0.75F;
        }
        if (other instanceof FuelRodBlockEntity) {
            return rest ? 0.2F : 1F;
        }
        return super.heatConductionEfficiency(other);
    }

    /** dumpFuel: gives a fuel dump valve up to {@code max} mB of fuel. */
    int dumpFuel(int max) {
        int amount = Math.min(max, fuel.getFluidAmount());
        if (amount > 0) {
            removeLiquid(fuel, amount);
        }
        return amount;
    }

    public boolean hasFuel() {
        return fuel.getFluidAmount() >= CYCLE_AMOUNT;
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        super.onNeutron(neutron, level, pos);
        if (!level.isClientSide && neutron.neutronType().canTriggerFission(level.random)
                && Chance.of(level.random, neutron.neutronSpeed().interactionMultiplier()) && neutron.neutronType() != NeutronType.BREEDER
                && Chance.of(level.random, neutronInteractionChance())) {
            if (isPoisoned()) {
                return true;
            }
            if (Chance.of(level.random, neutronChance()) && hasFuel()) {
                removeLiquid(fuel, CYCLE_AMOUNT);
                addLiquid(hotFuel, ReactorFluids.HOT_LIFBE.get(), CYCLE_AMOUNT);
                temperature += 50;
                spawnNeutronBurst();
                if (Chance.of(level.random, 5)) {
                    addWaste();
                }
            }
            return true;
        }
        return false;
    }

    /** Cosine curve peaking in the middle of the working range, flatter below it (0.5) than above (0.75). */
    private double neutronInteractionChance() {
        int mid = (MIN_TEMPERATURE + MAX_TEMPERATURE) / 2;
        double f = temperature <= mid ? 0.5 : 0.75;
        return 1 - f + f * cosInterpolation(MIN_TEMPERATURE, MAX_TEMPERATURE, temperature);
    }

    private static double cosInterpolation(double min, double max, double value) {
        if (value < min || value > max) {
            return 0;
        }
        double size = (max - min) / 2;
        double mid = min + size;
        return value == mid ? 1 : 0.5 + 0.5 * Math.cos(Math.toRadians((value - mid) / size * 180));
    }

    private double neutronChance() {
        return 50 - 40 * Math.sqrt((temperature - MIN_TEMPERATURE) / (double) (MAX_TEMPERATURE - MIN_TEMPERATURE));
    }

    /** Liquid waste poisons it: up to 0.875 chance as the waste tank fills. */
    @Override
    protected boolean isPoisoned() {
        return Chance.of(level.random, 0.875 * Math.pow(waste.getFluidAmount() / (double) waste.getCapacity(), 1.6));
    }

    @Override
    protected void addWaste() {
        addLiquid(waste, ReactorFluids.NUCLEAR_WASTE.get(), CYCLE_AMOUNT / 2);
    }

    @Override
    public boolean isFissile() {
        return false;
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected boolean canRemove(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canDumpHeatInto(CoolantState coolant) {
        return coolant == CoolantState.LITHIUM;
    }

    @Override
    public ReactorType getReactorType() {
        return ReactorType.THORIUM;
    }

    public FluidTank fuelTank() {
        return fuel;
    }

    public FluidTank hotFuelTank() {
        return hotFuel;
    }

    public FluidTank wasteTank() {
        return waste;
    }

    /** Fuel salt comes in from the top, hot fuel out of the bottom, waste out of the sides. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        if (side == Direction.UP) {
            return fuelIn;
        }
        return side == Direction.DOWN ? hotOut : wasteOut;
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return new int[0];
    }

    // ---- GUI: three tanks and no slots ----

    @Override
    public void addMenuSlots(ReactorMenu menu) {
    }

    @Override
    public boolean hasPlayerInventory() {
        return false;
    }
}
