package net.scwunge.reactorcraft.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.scwunge.reactorcraft.content.block.FluoriteBlock;
import net.scwunge.reactorcraft.content.block.FluoriteOreBlock;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.NeutronType.NeutronSpeed;
import net.scwunge.reactorcraft.core.RadiationHooks;
import net.scwunge.reactorcraft.core.RadiationShield;
import net.scwunge.reactorcraft.registry.ReactorEntities;

import java.util.List;

/**
 * A neutron (EntityNeutron): flies in a straight line at 0.75 blocks a tick, passing through air, and reacts with every
 * block it enters: block entities that take neutrons (fuel, water, control rods...) decide for themselves; fluorite
 * lights up; shielding and dense blocks absorb it, sometimes leaving a little radiation behind. It fades out after six
 * seconds or so.
 */
public class NeutronEntity extends Entity implements IEntityWithComplexSpawn {
    public static final double SPEED = 0.75;
    private static final double HITBOX = 0.1;

    private NeutronType type = NeutronType.NULL;
    private NeutronSpeed speed = NeutronSpeed.THERMAL;
    private BlockPos spawnBlock = BlockPos.ZERO;
    private BlockPos lastBlock = BlockPos.ZERO;

    public NeutronEntity(EntityType<? extends NeutronEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    /** Spawns a neutron at the middle of {@code pos}, flying {@code direction}. */
    public NeutronEntity(Level level, BlockPos pos, Direction direction, NeutronType type) {
        this(ReactorEntities.NEUTRON.get(), level);
        this.type = type;
        this.speed = type.creationSpeed();
        this.spawnBlock = pos;
        this.lastBlock = pos;
        setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        setDeltaMovement(direction.getStepX() * SPEED, direction.getStepY() * SPEED, direction.getStepZ() * SPEED);
    }

    public NeutronType neutronType() {
        return type;
    }

    public NeutronSpeed neutronSpeed() {
        return speed;
    }

    /** moderate: a moderator (heavy water) slows it to thermal speed. */
    public void moderate() {
        speed = NeutronSpeed.THERMAL;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        Vec3 motion = getDeltaMovement();
        if (!level.isClientSide) {
            if (motion.equals(Vec3.ZERO) && tickCount > 20 || getY() > level.getMaxBuildHeight() || getY() < level.getMinBuildHeight()) {
                discard();
                return;
            }
            if (tickCount > 120 && Chance.of(random, tickCount - 120)) {
                discard();
                return;
            }
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (level.isClientSide) {
            return;
        }
        BlockPos now = blockPosition();
        if (!now.equals(lastBlock)) {
            lastBlock = now;
            // it never reacts with the block it was fired from
            if (!now.equals(spawnBlock) && onEnterBlock(level, now)) {
                discard();
                return;
            }
        }
        List<Entity> hit = level.getEntities(this, new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(HITBOX));
        for (Entity e : hit) {
            if (applyEntityCollision(e)) {
                discard();
                return;
            }
        }
    }

    /** applyEntityCollision: living things have a one in eight chance of being hit, which stops the neutron. */
    private boolean applyEntityCollision(Entity e) {
        if (e instanceof LivingEntity living && Chance.of(random, 12.5)) {
            RadiationHooks.pulse(living);
            return true;
        }
        return false;
    }

    /** Returns true if the neutron is absorbed. */
    private boolean onEnterBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return random.nextInt(1000) == 0;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NeutronTile tile) {
            return tile.onNeutron(this, level, pos);
        }
        if (state.getBlock() instanceof FluoriteBlock fluorite) {
            fluorite.activate(level, pos, state);
        } else if (state.getBlock() instanceof FluoriteOreBlock ore) {
            ore.activate(level, pos, state);
        }
        RadiationShield shield = RadiationShield.of(state);
        if (shield != null && Chance.of(random, shield.neutronAbsorbChance)) {
            return true;
        }
        if (Chance.of(random, speed.irradiatedAbsorptionChance())) {
            if (absorbedByMaterial(level, pos, state)) {
                spawnRadiationChance(level, pos);
                if (Chance.of(random, 20)) {
                    RadiationHooks.transformBlock(level, pos);
                }
                return true;
            }
        }
        return false;
    }

    /** Dense, opaque, hard blocks soak neutrons up; thin ones rarely do. */
    private boolean absorbedByMaterial(Level level, BlockPos pos, BlockState state) {
        float resistance = state.getBlock().getExplosionResistance();
        // the original's light opacity is 0-255; 1.21's is 0-15
        int opacity = state.getLightBlock(level, pos) * 17;
        if (state.isSolidRender(level, pos)) {
            int bound = (int) (24 - resistance);
            return random.nextBoolean() && resistance >= 12 || (bound > 1 ? random.nextInt(bound) : 0) == 0;
        }
        if (opacity == 255) {
            return (opacity > 1 ? random.nextInt(opacity) : 0) > 0;
        }
        return random.nextInt(1000) == 0;
    }

    private void spawnRadiationChance(Level level, BlockPos pos) {
        if (Chance.of(random, 2)) {
            RadiationHooks.spawnLowRadiation(level, pos);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        type = NeutronType.values()[Math.min(tag.getInt("NType"), NeutronType.values().length - 1)];
        speed = NeutronSpeed.values()[Math.min(tag.getInt("NSpeed"), NeutronSpeed.values().length - 1)];
        spawnBlock = BlockPos.of(tag.getLong("Spawn"));
        lastBlock = blockPosition();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("NType", type.ordinal());
        tag.putInt("NSpeed", speed.ordinal());
        tag.putLong("Spawn", spawnBlock.asLong());
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());
        buf.writeVarInt(speed.ordinal());
        buf.writeBlockPos(spawnBlock);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        type = NeutronType.values()[buf.readVarInt()];
        speed = NeutronSpeed.values()[buf.readVarInt()];
        spawnBlock = buf.readBlockPos();
        lastBlock = spawnBlock;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance <= 4096;
    }
}
