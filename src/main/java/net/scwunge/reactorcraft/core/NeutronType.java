package net.scwunge.reactorcraft.core;

import net.minecraft.util.RandomSource;
import net.scwunge.reactorcraft.ReactorConfig;

/** Where a neutron came from, which decides what it can do. */
public enum NeutronType {
    NULL,
    DECAY,
    FISSION,
    BREEDER,
    FUSION,
    WASTE,
    THORIUM;

    public int boilerAbsorptionChance() {
        return this == BREEDER || this == THORIUM ? 80 : 0;
    }

    public int sodiumBoilerAbsorptionChance() {
        return this != BREEDER && this != DECAY ? 90 : 0;
    }

    public boolean canTriggerFuelConversion() {
        return this == BREEDER;
    }

    public boolean dealsDamage() {
        return this != NULL;
    }

    public boolean stoppedByWater() {
        return this != FUSION;
    }

    public boolean canIrradiateMaterials() {
        return this == FISSION || this == FUSION || this == BREEDER || this == THORIUM;
    }

    public boolean isFissionType() {
        return this == DECAY || this == FISSION || this == BREEDER || this == THORIUM;
    }

    public boolean canTriggerFission(RandomSource random) {
        return isFissionType() || this == WASTE && Chance.of(random, 40);
    }

    public NeutronSpeed creationSpeed() {
        if (!ReactorConfig.FAST_NEUTRONS.get()) {
            return NeutronSpeed.THERMAL;
        }
        return switch (this) {
            case BREEDER, FISSION, THORIUM -> NeutronSpeed.FAST;
            default -> NeutronSpeed.THERMAL;
        };
    }

    /** Fast neutrons react less often until moderated. */
    public enum NeutronSpeed {
        THERMAL,
        FAST;

        public float interactionMultiplier() {
            return this == FAST ? 0.6F : 1;
        }

        public double irradiatedAbsorptionChance() {
            return this == FAST ? 40 : 100;
        }

        public float wasteConversionMultiplier() {
            return this == FAST ? 2.2F : 1;
        }
    }
}
