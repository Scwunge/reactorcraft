package net.scwunge.reactorcraft.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.scwunge.reactorcraft.core.RadiationEffects;
import net.scwunge.reactorcraft.core.RadiationIntensity;
import net.scwunge.reactorcraft.registry.ReactorEntities;

/**
 * A patch of radiation (EntityRadiation): it irradiates every living thing within its range each tick and now and then
 * withers a block near it. It slowly fades (about once in 18 minutes it loses a block of range; in rain, faster) and water tools
 * wash it away. An explosion scatters it again.
 */
public class RadiationEntity extends Entity implements IEntityWithComplexSpawn {
    private int range;
    private RadiationIntensity intensity = RadiationIntensity.LOWLEVEL;

    public RadiationEntity(EntityType<? extends RadiationEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public RadiationEntity(Level level, int range, RadiationIntensity intensity) {
        this(ReactorEntities.RADIATION.get(), level);
        this.range = range;
        this.intensity = intensity;
    }

    public int range() {
        return range;
    }

    public RadiationIntensity intensity() {
        return intensity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        applyRadiation();
        if (range <= 0) {
            discard();
            return;
        }
        if (random.nextInt(360000) == 0) {
            clean();
        }
        if (level().isRaining() && random.nextInt(36000) == 0 && level().getBiome(blockPosition()).value().hasPrecipitation()) {
            clean();
        }
    }

    private void applyRadiation() {
        Level level = level();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(range))) {
            if (e.position().distanceTo(position()) <= range) {
                RadiationEffects.applyEffects(e, intensity);
            }
        }
        BlockPos at = blockPosition().offset(random.nextIntBetweenInclusive(-range, range), random.nextIntBetweenInclusive(-range, range),
                random.nextIntBetweenInclusive(-range, range));
        RadiationEffects.transformBlock(level, at, intensity, null);
    }

    /** Washing: one block less range, and gone at none. */
    public void clean() {
        if (range > 0) {
            range--;
        } else {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION) && !level().isClientSide) {
            RadiationEffects.contaminateArea(level(), blockPosition(), range, 0.65F, 0.5, true, intensity);
            discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance <= 4096;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        range = tag.getInt("Range");
        intensity = RadiationIntensity.values()[Math.min(tag.getInt("Intensity"), RadiationIntensity.values().length - 1)];
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Range", range);
        tag.putInt("Intensity", intensity.ordinal());
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(range);
        buf.writeVarInt(intensity.ordinal());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        range = buf.readVarInt();
        intensity = RadiationIntensity.values()[buf.readVarInt()];
    }
}
