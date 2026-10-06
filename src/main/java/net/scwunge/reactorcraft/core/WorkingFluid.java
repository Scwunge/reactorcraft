package net.scwunge.reactorcraft.core;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import org.jetbrains.annotations.Nullable;

/**
 * What a reactor boils into steam (the original's WorkingFluid): water (and heavy water, which boils as water) or ammonia,
 * which boils at -33 C and drives a turbine twice as hard, but explodes if the boiler gets to 650 C.
 */
public enum WorkingFluid {
    EMPTY(0, 0),
    WATER(1F, 100),
    AMMONIA(2F, -33);

    public final float efficiency;
    public final int boilingTemperature;

    WorkingFluid(float efficiency, int boilingTemperature) {
        this.efficiency = efficiency;
        this.boilingTemperature = boilingTemperature;
    }

    /** The liquid it is before it boils. */
    @Nullable
    public Fluid fluid() {
        return switch (this) {
            case WATER -> Fluids.WATER;
            case AMMONIA -> ReactorFluids.AMMONIA.get();
            default -> null;
        };
    }

    /** The liquid steam of this fluid condenses into. */
    @Nullable
    public Fluid lowPressureFluid() {
        return switch (this) {
            case WATER -> ReactorFluids.LOW_PRESSURE_WATER.get();
            case AMMONIA -> ReactorFluids.LOW_PRESSURE_AMMONIA.get();
            default -> null;
        };
    }

    /** The working fluid a liquid makes, or null: water, heavy water and ammonia work. */
    @Nullable
    public static WorkingFluid of(@Nullable Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        if (fluid == ReactorFluids.HEAVY_WATER.get()) {
            return WATER;
        }
        for (WorkingFluid w : values()) {
            if (w != EMPTY && fluid == w.fluid()) {
                return w;
            }
        }
        return null;
    }

    public static WorkingFluid byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : EMPTY;
    }
}
