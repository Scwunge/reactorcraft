package net.scwunge.reactorcraft.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.registry.ReactorEntities;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A bit of fusion plasma (EntityPlasma): flies level at 0.75 blocks a tick from an injector, steered round the ring by the toroid magnets. It
 * kills whatever it touches and lights flammable blocks. Where fifteen or more come together they fuse, giving off three fusion neutrons. One that
 * no magnet has held for a while has escaped, and dies a few seconds later.
 */
public class PlasmaEntity extends Entity {
    public static final double SPEED = 0.75;
    public static final ResourceKey<DamageType> FUSION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ReactorCraft.id("fusion"));
    private static final double DESPAWN_DISTANCE = 100;

    private int targetX;
    private int targetZ;
    private double spawnY;
    private BlockPos lastBlock = BlockPos.ZERO;
    private Vec3 origin = Vec3.ZERO;
    @Nullable
    private UUID owner;
    private int escapeTicks;
    /** The ordinal of the last magnet that steered it, or -1. */
    public int magnetOrdinal = -1;

    public PlasmaEntity(EntityType<? extends PlasmaEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Plasma at the middle of {@code pos}, made in the name of {@code owner}. */
    public PlasmaEntity(Level level, BlockPos pos, @Nullable UUID owner) {
        this(ReactorEntities.PLASMA.get(), level);
        this.owner = owner;
        this.spawnY = pos.getY() + 0.5;
        this.lastBlock = pos;
        setPos(pos.getX() + 0.5, spawnY, pos.getZ() + 0.5);
        this.origin = position();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Turns it to fly towards the middle of the block column (x, z). */
    public void setTarget(int x, int z) {
        targetX = x;
        targetZ = z;
        double dx = targetX + 0.5 - getX();
        double dz = targetZ + 0.5 - getZ();
        double dd = Math.sqrt(dx * dx + dz * dz);
        if (dd > 1.0E-4) {
            setDeltaMovement(dx * SPEED / dd, 0, dz * SPEED / dd);
            hurtMarked = true;
        }
    }

    public void resetEscapeTimer() {
        escapeTicks = 0;
    }

    public boolean hasEscaped() {
        return escapeTicks >= 6;
    }

    public boolean hasEscapedSeverely() {
        return escapeTicks >= 12;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, 0, motion.z);
        setPos(getX() + motion.x, spawnY, getZ() + motion.z);
        escapeTicks++;
        if (level().isClientSide) {
            return;
        }
        if (position().distanceToSqr(origin) > DESPAWN_DISTANCE * DESPAWN_DISTANCE || tickCount > 300 && hasEscapedSeverely()) {
            discard();
            return;
        }
        BlockPos now = blockPosition();
        if (!now.equals(lastBlock)) {
            lastBlock = now;
            Thermal.igniteAt(level(), now, owner);
        }
        for (Entity e : level().getEntities(this, new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(0.5))) {
            if (!(e instanceof PlasmaEntity) && !(e instanceof NeutronEntity)) {
                burn(e);
            }
        }
        if (!hasEscapedSeverely() && random.nextInt(hasEscaped() ? 48 : 12) == 0) {
            checkFusion();
        }
    }

    /** applyEntityCollision: fatal, unless fire resistance cuts it to four. */
    private void burn(Entity e) {
        float damage = e instanceof LivingEntity living && living.hasEffect(MobEffects.FIRE_RESISTANCE) ? 4 : Float.MAX_VALUE;
        e.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FUSION_DAMAGE)), damage);
    }

    public int fusionThreshold() {
        return 15 + random.nextInt(6);
    }

    /** Enough plasma packed together fuses: three fusion neutrons go off in random horizontal directions. */
    private void checkFusion() {
        List<PlasmaEntity> near = level().getEntitiesOfClass(PlasmaEntity.class, getBoundingBox().inflate(1));
        if (near.size() >= fusionThreshold() && !near.get(0).hasEscaped() && !near.get(near.size() - 1).hasEscaped()) {
            fuse();
        }
    }

    private void fuse() {
        Direction[] horizontal = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int i = 0; i < 3; i++) {
            level().addFreshEntity(new NeutronEntity(level(), blockPosition(), horizontal[random.nextInt(4)], NeutronType.FUSION));
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS, 1F, 1F);
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        targetX = tag.getInt("TargetX");
        targetZ = tag.getInt("TargetZ");
        spawnY = tag.getDouble("SpawnY");
        escapeTicks = tag.getInt("Escape");
        magnetOrdinal = tag.contains("Magnet") ? tag.getInt("Magnet") : -1;
        origin = new Vec3(tag.getDouble("OriginX"), spawnY, tag.getDouble("OriginZ"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TargetX", targetX);
        tag.putInt("TargetZ", targetZ);
        tag.putDouble("SpawnY", spawnY);
        tag.putInt("Escape", escapeTicks);
        tag.putInt("Magnet", magnetOrdinal);
        tag.putDouble("OriginX", origin.x);
        tag.putDouble("OriginZ", origin.z);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
