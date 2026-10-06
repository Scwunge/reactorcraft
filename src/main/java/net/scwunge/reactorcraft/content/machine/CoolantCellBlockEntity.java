package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.Temperatured;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import org.jetbrains.annotations.Nullable;

/**
 * Coolant Cell (TileEntityWaterCell): a block full of water, heavy water, sodium or lithium beryllium fluoride. It is filled a
 * bucketful (1000 mB) at a time from a tank above it, passes its contents down to an empty cell below it, takes heat from hot
 * neighbours (and sometimes boils off when it does) and, full of heavy water, slows neutrons down to thermal speed.
 */
public class CoolantCellBlockEntity extends ReactorMachineBlockEntity implements Temperatured, NeutronTile {
    public static final int BUCKET = 1000;
    public static final int MAX_TEMPERATURE = 1000;

    private CoolantState coolant = CoolantState.EMPTY;

    public CoolantCellBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.COOLANT_CELL.get(), pos, state, 0);
    }

    public CoolantState coolant() {
        return coolant;
    }

    public void setCoolant(CoolantState state) {
        if (coolant != state) {
            coolant = state;
            if (level != null && !level.isClientSide) {
                level.setBlock(worldPosition, getBlockState().setValue(CoolantCellBlock.COOLANT, state), 3);
            }
            setChanged();
        }
    }

    @Override
    protected void tickServer() {
        if (level.getBlockEntity(worldPosition.below()) instanceof CoolantCellBlockEntity below && below.coolant == CoolantState.EMPTY
                && coolant != CoolantState.EMPTY) {
            below.setCoolant(coolant);
            setCoolant(CoolantState.EMPTY);
        }
        if (coolant == CoolantState.EMPTY) {
            tryFill();
        }
        if (thermalStep()) {
            updateTemperature();
        }
    }

    /** From any tank above that can give a whole bucket of a coolant. */
    private void tryFill() {
        IFluidHandler above = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.above(), Direction.DOWN);
        if (above == null) {
            return;
        }
        FluidStack offered = above.drain(BUCKET, IFluidHandler.FluidAction.SIMULATE);
        CoolantState state = offered.isEmpty() ? CoolantState.EMPTY : stateOf(offered.getFluid());
        if (state != CoolantState.EMPTY && offered.getAmount() >= BUCKET) {
            above.drain(BUCKET, IFluidHandler.FluidAction.EXECUTE);
            setCoolant(state);
        }
    }

    /** The coolant a fluid makes, or EMPTY if it is no coolant. */
    public static CoolantState stateOf(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return CoolantState.WATER;
        } else if (fluid == ReactorFluids.HEAVY_WATER.get()) {
            return CoolantState.HEAVY;
        } else if (fluid == ReactorFluids.SODIUM.get()) {
            return CoolantState.SODIUM;
        } else if (fluid == ReactorFluids.LIFBE.get()) {
            return CoolantState.LITHIUM;
        }
        return CoolantState.EMPTY;
    }

    /** The reactor part heat exchange, then: warm cells cool towards the surroundings, and the cell takes half the heat of any hot neighbour it can cool. */
    @Override
    protected void updateTemperature() {
        super.updateTemperature();
        int dT = temperature - net.scwunge.reactorcraft.core.Thermal.ambient(level, worldPosition);
        if (dT > 0) {
            temperature -= dT / 8;
        }
        for (Direction dir : Direction.values()) {
            BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
            if (other instanceof Temperatured tr && coolant != CoolantState.HEAVY && coolant != CoolantState.EMPTY && tr.canDumpHeatInto(coolant)) {
                int t = tr.getTemperature();
                int dt = t - temperature;
                if (dt > 0) {
                    temperature += dt / 2;
                    tr.setTemperature(t - dt / 2);
                    if (level.random.nextInt(5) == 0) {
                        setCoolant(CoolantState.EMPTY);
                    }
                }
            }
        }
    }

    /** Heavy water moderates: a neutron through it drops to thermal speed. Neutrons are never absorbed here. */
    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        if (coolant == CoolantState.HEAVY) {
            neutron.moderate();
        }
        return false;
    }

    /** The share of neutrons the coolant would stop (unused by the original, kept for display). */
    public int chanceToStop() {
        return switch (coolant) {
            case HEAVY -> 75;
            case WATER -> 50;
            default -> 0;
        };
    }

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public boolean canDumpHeatInto(CoolantState other) {
        return other != CoolantState.EMPTY && coolant.isWater() == other.isWater();
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Coolant", coolant.ordinal());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        coolant = CoolantState.values()[Math.min(tag.getInt("Coolant"), CoolantState.values().length - 1)];
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Coolant", coolant.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        coolant = CoolantState.values()[Math.min(tag.getInt("Coolant"), CoolantState.values().length - 1)];
    }
}
