package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;
import net.scwunge.reactorcraft.core.ReactorPart;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.HeatConduction;
import net.scwunge.reactorcraft.core.ReactorTypeMix;
import net.scwunge.reactorcraft.core.ReactorTyped;
import net.scwunge.reactorcraft.core.TemperaturedReactorTyped;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

/**
 * The shared part of the boilers (TileEntityNuclearBoiler): a tank fed from below, steam made from it, a record of which kinds
 * of reactor its heat came from (steam keeps it, and turbines scale their output by it), and fluid shared with boilers stacked
 * above and below.
 */
public abstract class NuclearBoilerBlockEntity extends ReactorMachineBlockEntity implements TemperaturedReactorTyped, ReactorPart, NeutronTile, HeatConduction {
    protected int steam;
    protected final FluidTank tank;
    private final IFluidHandler fillOnly;
    private final ReactorTypeMix types = new ReactorTypeMix();

    protected NuclearBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, 0);
        this.tank = addTank("Tank", capacity, s -> isValidFluid(s.getFluid()));
        this.fillOnly = FluidAccess.fillOnly(tank);
        types.add(defaultReactorType(), 1);
    }

    /** A heat pipe may push heat into a boiler, but not take it back out. */
    @Override
    public boolean allowExternalHeating() {
        return true;
    }

    @Override
    public boolean allowHeatExtraction() {
        return false;
    }

    public abstract ReactorType defaultReactorType();

    protected abstract boolean isValidFluid(Fluid fluid);

    protected abstract void overheat();

    public final void addReactorType(@Nullable ReactorType type, double amount) {
        types.add(type, amount);
    }

    /** The kind of reactor most of its heat came from. */
    @Override
    public final ReactorType getReactorType() {
        ReactorType largest = types.largest();
        return largest != null ? largest : defaultReactorType();
    }

    public final Iterable<ReactorType> reactorTypes() {
        return types.types();
    }

    public final double reactorTypeFraction(ReactorType type) {
        return types.fraction(type);
    }

    @Override
    protected void tickServer() {
        if (thermalStep()) {
            updateTemperature();
        }
        balanceFluid();
    }

    @Override
    protected void updateTemperature() {
        super.updateTemperature();
        if (temperature > getMaxTemperature()) {
            overheat();
        }
    }

    @Override
    protected void onHeatReceived(ReactorBlockEntity from, double amount) {
        if (from instanceof NuclearBoilerBlockEntity other && other.getClass() == getClass()) {
            addReactorType(other.getReactorType(), amount);
        } else if (!(from instanceof NuclearBoilerBlockEntity) && from instanceof ReactorTyped typed) {
            addReactorType(typed.getReactorType(), amount);
        }
    }

    /** balanceFluid: a quarter of the difference flows into a boiler above or below that has less. */
    protected void balanceFluid() {
        for (Direction dir : new Direction[]{Direction.DOWN, Direction.UP}) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof NuclearBoilerBlockEntity other && other.getType() == getType()) {
                if (other.tank.getFluidAmount() < tank.getFluidAmount() && (other.tank.isEmpty() || other.tank.getFluid().is(fluidOf(tank)))) {
                    int dl = tank.getFluidAmount() - other.tank.getFluidAmount();
                    int amount = dl / 4 + 1;
                    addLiquid(other.tank, fluidOf(tank), amount);
                    removeLiquid(tank, amount);
                }
            }
        }
    }

    public final int removeSteam() {
        int s = steam;
        steam = 0;
        return s;
    }

    public final void addLiquid(int amount, Fluid fluid) {
        addLiquid(tank, fluid, amount);
    }

    public FluidTank tank() {
        return tank;
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, net.minecraft.world.level.Level level, BlockPos pos) {
        return false;
    }

    @Override
    public final int getTemperature() {
        return temperature;
    }

    @Override
    public final void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    @Override
    public final boolean canDumpHeatInto(CoolantState coolant) {
        return false;
    }

    /** Fluid goes in from below only. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side == Direction.DOWN ? fillOnly : null;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Steam", steam);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        steam = tag.getInt("Steam");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Steam", steam);
        CompoundTag mix = new CompoundTag();
        types.save(mix);
        tag.put("Types", mix);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam = tag.getInt("Steam");
        if (tag.contains("Types")) {
            types.load(tag.getCompound("Types"));
            if (types.isEmpty()) {
                types.add(defaultReactorType(), 1);
            }
        }
    }
}
