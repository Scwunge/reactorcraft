package net.scwunge.reactorcraft.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.core.RadiationEffects;
import net.scwunge.reactorcraft.core.RadiationIntensity;
import net.scwunge.reactorcraft.registry.ReactorEntities;

/**
 * Nuclear waste lying around (EntityNuclearWaste): it never despawns or burns, irradiates (high-level) anything within 6
 * blocks, and contaminates the ground around it after three minutes and every ten minutes after that. It cannot fall out of the
 * world: below the bottom it is pushed back up, contaminating as it goes.
 */
public final class NuclearWasteEntity extends ItemEntity {
    public static final int RANGE = 6;
    public static final int RADIATION_INTERVAL = 10 * 60 * 20;
    public static final int RADIATION_DELAY = 3 * 60 * 20;
    private int timer;

    public NuclearWasteEntity(EntityType<? extends NuclearWasteEntity> type, Level level) {
        super(type, level);
    }

    public NuclearWasteEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(ReactorEntities.NUCLEAR_WASTE_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        setUnlimitedLifetime();
        applyRadiation();
        if (getY() < level().getMinBuildHeight()) {
            setDeltaMovement(getDeltaMovement().x, Math.abs(getDeltaMovement().y), getDeltaMovement().z);
            setPos(getX(), Math.max(getY(), level().getMinBuildHeight()), getZ());
            hurtMarked = true;
            if (timer % 256 == 0 && level().getEntitiesOfClass(RadiationEntity.class, new AABB(blockPosition()).inflate(RANGE)).size() < 100) {
                RadiationEffects.contaminateArea(level(), blockPosition(), RANGE * 4, 2, 0, false, RadiationIntensity.HIGHLEVEL);
            }
        }
        timer++;
    }

    private void applyRadiation() {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(RANGE))) {
            if (e.position().distanceTo(position()) <= RANGE) {
                RadiationEffects.applyEffects(e, RadiationIntensity.HIGHLEVEL);
            }
        }
        if (timer >= RADIATION_DELAY && (timer - RADIATION_DELAY) % RADIATION_INTERVAL == 0) {
            BlockPos at = blockPosition();
            if (level().getEntitiesOfClass(RadiationEntity.class, new AABB(at).inflate(RANGE)).size() < 32) {
                RadiationEffects.contaminateArea(level(), at, RANGE * 4, 2, 0, false, RadiationIntensity.HIGHLEVEL);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }
}
