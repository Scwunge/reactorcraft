package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;

/**
 * Carbon Dioxide Heat Exchanger (TileEntityCO2Heater): the pebble bed's boiler. Above 800 C it heats carbon dioxide (100 mB a second,
 * taking 117 degrees out of itself) into hot carbon dioxide for a heat exchanger; at 3000 C it explodes.
 */
public class Co2HeaterBlockEntity extends IntermediateBoilerBlockEntity {
    public static final int CAPACITY = 12000;
    public static final int MAX_TEMPERATURE = 3000;
    /** ReikaThermoHelper.CO2_HEAT */
    public static final double CO2_HEAT = 1.168;

    public Co2HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.CO2_HEATER.get(), pos, state, CAPACITY);
    }

    @Override
    public ReactorType defaultReactorType() {
        return ReactorType.HTGR;
    }

    @Override
    protected int liquidUsage() {
        return 100;
    }

    @Override
    protected int minimumTemperature() {
        return PebbleBedBlockEntity.MIN_TEMPERATURE;
    }

    @Override
    protected double fluidHeatCapacity() {
        return CO2_HEAT;
    }

    @Override
    protected Fluid inputFluid() {
        return ReactorFluids.CO2.get();
    }

    @Override
    protected Fluid outputFluid() {
        return ReactorFluids.HOT_CO2.get();
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    protected void overheat() {
        if (ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner())) {
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 4F, true,
                    Level.ExplosionInteraction.BLOCK);
        } else {
            temperature = MAX_TEMPERATURE;
        }
    }
}
