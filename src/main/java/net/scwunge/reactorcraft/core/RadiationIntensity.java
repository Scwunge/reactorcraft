package net.scwunge.reactorcraft.core;

import net.minecraft.world.entity.LivingEntity;

/**
 * How strong radiation is (the original RadiationIntensity), with how long the effect it gives lasts, in ticks, and what
 * protects against it: hazmat suits stop high-level, hazmat or bedrock armor stops moderate, and for low-level any dense armor
 * will do. Background is harmless; lethal (meltdowns) cannot be stopped.
 */
public enum RadiationIntensity {
    BACKGROUND(0),
    LOWLEVEL(100),
    MODERATE(1200),
    HIGHLEVEL(6000),
    LETHAL(36000);

    public final int potionDuration;

    RadiationIntensity(int potionDuration) {
        this.potionDuration = potionDuration;
    }

    public boolean causesHarm() {
        return this != BACKGROUND;
    }

    public boolean isAtLeast(RadiationIntensity other) {
        return ordinal() >= other.ordinal();
    }

    public boolean hasSufficientShielding(LivingEntity entity) {
        return switch (this) {
            case BACKGROUND -> true;
            case LETHAL -> false;
            case HIGHLEVEL -> RadiationEffects.wearsFullSuit(entity, RadiationEffects.HAZMAT);
            case MODERATE -> RadiationEffects.wearsFullSuit(entity, RadiationEffects.HAZMAT, RadiationEffects.BEDROCK);
            case LOWLEVEL -> RadiationEffects.wearsFullSuit(entity, RadiationEffects.HAZMAT, RadiationEffects.BEDROCK, RadiationEffects.DENSE);
        };
    }
}
