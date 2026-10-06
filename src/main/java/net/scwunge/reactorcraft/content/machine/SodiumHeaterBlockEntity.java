package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;

/**
 * Sodium Heater (TileEntitySodiumHeater): the breeder reactor's boiler. It heats molten sodium (100 mB a second, taking 123 degrees out of
 * itself, and only above 301 C) into superheated sodium for a heat exchanger. Sodium in it soaks up most neutrons; at 2000 C it explodes.
 */
public class SodiumHeaterBlockEntity extends IntermediateBoilerBlockEntity {
    public static final int CAPACITY = 12000;
    public static final int MAX_TEMPERATURE = 2000;
    /** ReikaThermoHelper.SODIUM_HEAT */
    public static final double SODIUM_HEAT = 1.23;

    public SodiumHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.SODIUM_HEATER.get(), pos, state, CAPACITY);
    }

    @Override
    public ReactorType defaultReactorType() {
        return ReactorType.BREEDER;
    }

    @Override
    protected int liquidUsage() {
        return 100;
    }

    @Override
    protected int minimumTemperature() {
        return 301;
    }

    @Override
    protected double fluidHeatCapacity() {
        return SODIUM_HEAT;
    }

    @Override
    protected Fluid inputFluid() {
        return ReactorFluids.SODIUM.get();
    }

    @Override
    protected Fluid outputFluid() {
        return ReactorFluids.HOT_SODIUM.get();
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

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        return !tank.isEmpty() && Chance.of(level.random, neutron.neutronType().sodiumBoilerAbsorptionChance());
    }
}
