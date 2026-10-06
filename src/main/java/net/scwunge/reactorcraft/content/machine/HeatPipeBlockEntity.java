package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.core.HeatConduction;
import net.scwunge.reactorcraft.core.HeatPipes;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.ReactorTyped;
import net.scwunge.reactorcraft.core.ReactorTypeMix;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;

/**
 * Heat Pipe (TileEntityHeatPipe): holds heat energy and shares it evenly with the heat pipes around it, with no loss along the way. It draws heat from
 * hotter parts that allow it (a reactor core, a heat exchanger) and gives heat to cooler parts that accept it (a boiler), a quarter of the difference
 * at a time, remembering which kinds of reactor the heat it carries came from so a boiler can pass that on to its steam.
 */
public class HeatPipeBlockEntity extends ReactorMachineBlockEntity {
    private double heatEnergy;
    private ReactorTypeMix types = new ReactorTypeMix();
    private int age;

    public HeatPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.HEAT_PIPE.get(), pos, state, 0);
    }

    @Override
    protected void onFirstTick() {
        if (heatEnergy <= 0) {
            heatEnergy = Thermal.ambient(level, worldPosition) * HeatPipes.CAPACITY;
        }
    }

    @Override
    protected void tickServer() {
        age++;
        balanceHeat();
        temperature = (int) (heatEnergy / HeatPipes.CAPACITY);
    }

    public double heatEnergy() {
        return heatEnergy;
    }

    public void setHeatEnergy(double heat) {
        heatEnergy = heat;
        temperature = (int) (heat / HeatPipes.CAPACITY);
        setChanged();
    }

    public int getTemperature() {
        return temperature;
    }

    private double pipeTemperature() {
        return heatEnergy / HeatPipes.CAPACITY;
    }

    private void balanceHeat() {
        for (Direction dir : DIRS) {
            BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
            if (other instanceof HeatPipeBlockEntity pipe) {
                balanceWith(pipe);
            } else if (other instanceof HeatConduction conduction && (conduction.allowExternalHeating() || conduction.allowHeatExtraction())) {
                exchangeWith(other, conduction);
            }
        }
    }

    /** Takes half the surplus from a pipe with more heat, and its record of where the heat came from. */
    private void balanceWith(HeatPipeBlockEntity other) {
        if (other.age < 2) {
            return;
        }
        double diff = other.heatEnergy - heatEnergy;
        if (diff <= 0) {
            return;
        }
        diff /= 2;
        other.heatEnergy -= diff;
        heatEnergy += diff;
        types = other.types.copy();
    }

    /** Moves a quarter of the temperature difference, as heat, between the pipe and a part: out of the part if it is hotter and allows it, in if cooler. */
    private void exchangeWith(BlockEntity other, HeatConduction part) {
        double ours = pipeTemperature();
        double theirs = part.getTemperature();
        double perDegree = Math.min(HeatPipes.CAPACITY, part.heatEnergyPerDegree());
        if (theirs > ours && part.allowHeatExtraction()) {
            double heat = (theirs - ours) / 4 * perDegree;
            part.setTemperature(part.getTemperature() - (int) Math.round(heat / part.heatEnergyPerDegree()));
            heatEnergy += heat;
            if (other instanceof ReactorTyped typed && typed.getReactorType() != null) {
                types.add(typed.getReactorType(), heat);
            }
        } else if (ours > theirs && part.allowExternalHeating()) {
            double heat = Math.min(heatEnergy, (ours - theirs) / 4 * perDegree);
            part.setTemperature(part.getTemperature() + (int) Math.round(heat / part.heatEnergyPerDegree()));
            heatEnergy -= heat;
            if (other instanceof NuclearBoilerBlockEntity boiler) {
                for (ReactorType type : types.types()) {
                    boiler.addReactorType(type, types.fraction(type) * heat);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("Heat", heatEnergy);
        CompoundTag mix = new CompoundTag();
        types.save(mix);
        tag.put("Types", mix);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heatEnergy = tag.getDouble("Heat");
        types.load(tag.getCompound("Types"));
    }
}
