package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.content.multi.FusionStructures;
import net.scwunge.reactorcraft.content.multi.MultiController;
import net.scwunge.reactorcraft.content.multi.MultiStructure;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.api.Laserable;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.Heatable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fusion Heater (TileEntityFusionHeater): at the middle of its chamber, it is heated (by a laser, or anything else that heats a machine) to
 * 150 million degrees, and then turns deuterium and tritium, fifty mB of each a tick, into a hundred mB of fusion plasma, which leaves by a magnetic pipe
 * on top. The chamber has to be built round it and sealed.
 */
public class FusionHeaterBlockEntity extends ReactorMachineBlockEntity implements MultiController, Heatable, PlasmaPort, Laserable {
    public static final int PLASMA_TEMPERATURE = 150_000_000;
    public static final int FEED_PER_BATCH = 50;
    public static final int PLASMA_PER_BATCH = 100;

    private final FluidTank plasma;
    private final FluidTank deuterium;
    private final FluidTank tritium;
    private final IFluidHandler feed;
    private final IFluidHandler plasmaOut;
    private final IFluidHandler anySide;
    private boolean formed;
    private long age;

    public FusionHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.FUSION_HEATER.get(), pos, state, 0);
        deuterium = addTank("Deuterium", 4000, s -> s.getFluid() == ReactorFluids.DEUTERIUM.get());
        tritium = addTank("Tritium", 4000, s -> s.getFluid() == ReactorFluids.TRITIUM.get());
        plasma = addTank("Plasma", 8000, s -> false);
        feed = new TankView(List.of(deuterium, tritium), List.of());
        plasmaOut = FluidAccess.drainOnly(plasma);
        anySide = new TankView(List.of(deuterium, tritium), List.of(plasma));
    }

    @Override
    protected void tickServer() {
        age++;
        int ambient = Thermal.ambient(level, worldPosition);
        int dT = temperature - ambient;
        if (dT != 0) {
            temperature -= (int) (1 + dT / 16384D);
        }
        if (!formed && age % 40 == 0) {
            structure().tryForm(level, worldPosition);
        }
        if (canMake()) {
            removeLiquid(deuterium, FEED_PER_BATCH);
            removeLiquid(tritium, FEED_PER_BATCH);
            addLiquid(plasma, ReactorFluids.FUSION_PLASMA.get(), PLASMA_PER_BATCH);
        }
    }

    /** The chamber must also be closed in: a heater with air against it does not count as built. */
    public boolean isBuilt() {
        return formed && !Thermal.isExposedToAir(level, worldPosition);
    }

    public boolean canMake() {
        return isBuilt() && temperature >= PLASMA_TEMPERATURE && deuterium.getFluidAmount() >= FEED_PER_BATCH && tritium.getFluidAmount() >= FEED_PER_BATCH
                && plasma.getCapacity() - plasma.getFluidAmount() >= PLASMA_PER_BATCH;
    }

    /** A laser of {@code power} watts shines on it: 640 degrees per bit of the power's logarithm, but only once it is built. */
    public void whenInBeam(long power) {
        if (isBuilt() && power > 1) {
            temperature += (int) (640 * (Math.log(power) / Math.log(2)));
        }
    }

    /** A Heat Ray or Laser Gun beam hits it: heats it, but only while the chamber is built round it. */
    @Override
    public void whenInBeam(Level level, BlockPos pos, long power, int step) {
        whenInBeam(power);
    }

    /** A built heater stops the beam. */
    @Override
    public boolean blockBeam(Level level, BlockPos pos, long power) {
        return isBuilt();
    }

    public FluidTank plasmaTank() {
        return plasma;
    }

    public FluidTank deuteriumTank() {
        return deuterium;
    }

    public FluidTank tritiumTank() {
        return tritium;
    }

    // ---- structure ----

    @Override
    public MultiStructure structure() {
        return FusionStructures.HEATER;
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public void setFormed(boolean formed) {
        if (this.formed != formed) {
            this.formed = formed;
            markForSync();
        }
    }

    // ---- fluid and heat ----

    /** Fuel in from the sides and below, plasma out of the top. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        return side == Direction.UP ? plasmaOut : feed;
    }

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public int getMaxTemperature() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature += amount;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        formed = tag.getBoolean("Formed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
    }
}
