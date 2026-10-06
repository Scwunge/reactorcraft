package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.ReactorPart;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.TemperaturedReactorTyped;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;

/**
 * Neutron Absorber (TileEntityNeutronAbsorber): soaks up fusion neutrons (40 degrees each) and melts into lava at 1500 C. It shares heat
 * with whatever coolant it touches.
 */
public class AbsorberBlockEntity extends ReactorMachineBlockEntity implements TemperaturedReactorTyped, ReactorPart, NeutronTile {
    public static final int MAX_TEMPERATURE = 1500;

    public AbsorberBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.ABSORBER.get(), pos, state, 0);
    }

    @Override
    protected void tickServer() {
        if (thermalStep()) {
            updateTemperature();
            if (temperature >= MAX_TEMPERATURE) {
                if (ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner())) {
                    level.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
                    level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
                } else {
                    temperature = MAX_TEMPERATURE - 1;
                }
            }
        }
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        if (neutron.neutronType() == NeutronType.FUSION) {
            temperature += 40;
            return true;
        }
        return false;
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
    public boolean canDumpHeatInto(CoolantState coolant) {
        return coolant != CoolantState.EMPTY;
    }

    @Override
    public ReactorType getReactorType() {
        return ReactorType.FUSION;
    }
}
