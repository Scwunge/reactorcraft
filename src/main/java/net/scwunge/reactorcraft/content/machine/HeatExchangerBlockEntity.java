package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.HeatConduction;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.StepTimer;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.Heatable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Heat Exchanger (TileEntityHeatExchanger): driven from below with 8192 W at 512 rad/s or more, it takes a hot working fluid in at the top, cools
 * a hundred mB of it a tick into its cold form (which collects in an output tank and leaves by the sides) and keeps the heat, which it gives to the
 * steam boilers beside it a quarter of the difference at a time.
 */
public class HeatExchangerBlockEntity extends ReactorMachineBlockEntity implements HeatConduction, Heatable {
    public static final int CAPACITY = 2000;
    public static final int MIN_TEMPERATURE = -140;
    public static final int MAX_TEMPERATURE = 1500;
    public static final int COOL_AMOUNT = 100;
    public static final int MIN_POWER = 8192;
    public static final int MIN_SPEED = 512;

    /** What a fluid cools into, how much heat it carries, up to what temperature the exchanger will take it and what kind of reactor it comes from. */
    public enum Exchange {
        SODIUM(() -> ReactorFluids.HOT_SODIUM.get(), () -> ReactorFluids.SODIUM.get(), 1, SodiumHeaterBlockEntity.SODIUM_HEAT, 600, ReactorType.BREEDER),
        CO2(() -> ReactorFluids.HOT_CO2.get(), () -> ReactorFluids.CO2.get(), 1, Co2HeaterBlockEntity.CO2_HEAT, PebbleBedBlockEntity.MIN_TEMPERATURE,
                ReactorType.HTGR),
        LIFBE(() -> ReactorFluids.HOT_LIFBE.get(), () -> ReactorFluids.LIFBE.get(), 1, 1.102, 1000, ReactorType.THORIUM),
        OXYGEN(() -> ReactorFluids.LIQUID_OXYGEN.get(), () -> ReactorFluids.OXYGEN.get(), 4, -(0.92 + 3.41 / 32), 500, ReactorType.NONE),
        SOLAR_SODIUM(() -> ReactorFluids.WARM_SODIUM.get(), () -> ReactorFluids.SODIUM.get(), 1, SodiumHeaterBlockEntity.SODIUM_HEAT * 0.375, 400,
                ReactorType.SOLAR);

        private final Supplier<Fluid> hot;
        private final Supplier<Fluid> cold;
        public final int expansionRatio;
        public final double heatCapacity;
        public final int maxTemperature;
        public final ReactorType type;

        Exchange(Supplier<Fluid> hot, Supplier<Fluid> cold, int ratio, double heatCapacity, int maxTemperature, ReactorType type) {
            this.hot = hot;
            this.cold = cold;
            this.expansionRatio = ratio;
            this.heatCapacity = heatCapacity;
            this.maxTemperature = maxTemperature;
            this.type = type;
        }

        public Fluid hotFluid() {
            return hot.get();
        }

        public Fluid coldFluid() {
            return cold.get();
        }

        @Nullable
        public static Exchange of(Fluid fluid) {
            for (Exchange e : values()) {
                if (e.hotFluid() == fluid) {
                    return e;
                }
            }
            return null;
        }
    }

    private final ShaftInput shaft = new ShaftInput();
    private final StepTimer timer = new StepTimer(20);
    private final FluidTank tank;
    private final FluidTank output;
    private final IFluidHandler fillIn;
    private final IFluidHandler drainOut;
    private final IFluidHandler anySide;
    @Nullable
    private Exchange recipe;

    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.HEAT_EXCHANGER.get(), pos, state, 0);
        tank = addTank("Tank", CAPACITY, s -> Exchange.of(s.getFluid()) != null);
        output = addTank("Output", CAPACITY, s -> false);
        fillIn = FluidAccess.fillOnly(tank);
        drainOut = FluidAccess.drainOnly(output);
        anySide = new TankView(List.of(tank), List.of(output));
    }

    @Override
    protected void tickServer() {
        shaft.read(level, worldPosition, Direction.DOWN);
        recipe = tank.isEmpty() ? null : Exchange.of(tank.getFluid().getFluid());
        if (canCool()) {
            cool();
        }
        if (thermalStep()) {
            distributeHeat();
            updateTemperature();
            temperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, temperature));
        }
    }

    public boolean hasPower() {
        return shaft.meets(MIN_POWER, MIN_SPEED, 1);
    }

    private boolean canCool() {
        return recipe != null && hasPower() && temperature < recipe.maxTemperature && tank.getFluidAmount() >= COOL_AMOUNT
                && output.getCapacity() - output.getFluidAmount() >= COOL_AMOUNT * recipe.expansionRatio
                && (output.isEmpty() || output.getFluid().getFluid() == recipe.coldFluid());
    }

    private void cool() {
        removeLiquid(tank, COOL_AMOUNT);
        addLiquid(output, recipe.coldFluid(), COOL_AMOUNT * recipe.expansionRatio);
        double efficiency = Math.min(1, Math.max(0.1, 1 - (temperature - 100) / (recipe.maxTemperature - 100D)));
        temperature = (int) Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, temperature + recipe.heatCapacity * COOL_AMOUNT * efficiency));
    }

    /** A quarter of the difference goes into each steam boiler beside it that is cooler, marked as coming from this fluid's kind of reactor. */
    private void distributeHeat() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof ReactorBoilerBlockEntity boiler) {
                int dT = temperature - boiler.getTemperature();
                if (dT > 0) {
                    int add = dT / 4;
                    temperature -= add;
                    boiler.setTemperature(boiler.getTemperature() + add);
                    boiler.addReactorType(recipe != null ? recipe.type : ReactorType.NONE, add);
                }
            }
        }
    }

    @Nullable
    public Exchange currentRecipe() {
        return recipe;
    }

    public FluidTank inputTank() {
        return tank;
    }

    public FluidTank outputTank() {
        return output;
    }

    /** Hot fluid in at the top, the cooled fluid out of the sides; nothing at the bottom, where the shaft is. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        return side == Direction.UP ? fillIn : side == Direction.DOWN ? null : drainOut;
    }

    // ---- heat ----

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    @Override
    public void addTemperature(int amount) {
        temperature += amount;
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public boolean canDumpHeatInto(CoolantState coolant) {
        return false;
    }

    @Override
    public boolean allowExternalHeating() {
        return false;
    }

    @Override
    public boolean allowHeatExtraction() {
        return true;
    }

    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    @Override
    public boolean canBeCooledWithFins() {
        return true;
    }
}
