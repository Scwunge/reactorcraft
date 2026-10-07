package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.rotarycraft.api.SodiumSolarUpgrades;
import net.scwunge.rotarycraft.power.Heatable;

/**
 * Solar Tower Sodium Cycler (TileEntitySolarTop): two of these stacked on top of a solar tower let it heat molten sodium instead of water. The tower tells it
 * how many mirrors shine on it and how brightly; it heats up to 1800 C and settles back towards the air temperature.
 */
public class SolarTopBlockEntity extends ReactorMachineBlockEntity implements SodiumSolarUpgrades.SodiumSolarReceiver, Heatable {
    public static final int MAX_TEMPERATURE = 1800;

    private long age;

    public SolarTopBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.SOLAR_TOP.get(), pos, state, 0);
    }

    /** Working: another of these above it and the tower (any other block entity) below. */
    @Override
    public boolean isActive() {
        return level != null && level.getBlockEntity(worldPosition.above()) instanceof SolarTopBlockEntity
                && level.getBlockEntity(worldPosition.below()) != null && !(level.getBlockEntity(worldPosition.below()) instanceof SolarTopBlockEntity);
    }

    @Override
    protected void tickServer() {
        age++;
        if (isActive() && age % 8 == 0) {
            int dT = Thermal.ambient(level, worldPosition) - temperature;
            if (dT != 0) {
                int diff = 1 + dT / 16;
                if (Math.abs(diff) <= 1) {
                    diff = Integer.signum(dT);
                }
                temperature += diff;
            }
        }
    }

    /** The tower's sunlight: warms it by a sixteenth of a degree for each mirror, scaled by how bright it is. */
    @Override
    public void tick(int mirrorCount, float totalBrightness) {
        if (level != null && !level.isClientSide) {
            temperature = Math.min(MAX_TEMPERATURE, temperature + (int) (0.0625 * 2 * mirrorCount * totalBrightness));
        }
    }

    /** The top block of the pair reports the temperature of the one below it when it is not the working one. */
    @Override
    public int getTemperature() {
        if (isActive()) {
            return temperature;
        }
        if (level != null && level.getBlockEntity(worldPosition.below()) instanceof SolarTopBlockEntity below) {
            return below.getTemperature();
        }
        return level == null ? temperature : Thermal.ambient(level, worldPosition);
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature += amount;
    }

    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }
}
