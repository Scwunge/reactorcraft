package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.block.FluoriteBlock;
import net.scwunge.reactorcraft.content.block.FluoriteOreBlock;
import net.scwunge.reactorcraft.content.entity.RadiationEntity;
import net.scwunge.reactorcraft.registry.ReactorEffects;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What radiation does (the original RadiationEffects): gives the radiation effect to things that are not shielded or in
 * creative, spreads contamination as {@link RadiationEntity radiation entities}, and withers plants and soil in the open air under high
 * radiation. Block changes honour the radiationTransformsBlocks option, mobGriefing and claims.
 */
public final class RadiationEffects {
    public static final ResourceKey<DamageType> RADIATION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ReactorCraft.id("radiation"));
    public static final TagKey<Item> HAZMAT = itemTag("radiation_protection/hazmat");
    public static final TagKey<Item> BEDROCK = itemTag("radiation_protection/bedrock");
    public static final TagKey<Item> DENSE = itemTag("radiation_protection/dense");

    private RadiationEffects() {
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ReactorCraft.MODID, path));
    }

    public static DamageSource radiationDamage(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RADIATION_DAMAGE));
    }

    // ---- who is protected ----

    /** Whether all four armor slots hold items from the tags. */
    @SafeVarargs
    public static boolean wearsFullSuit(LivingEntity entity, TagKey<Item>... tags) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                return false;
            }
            boolean ok = false;
            for (TagKey<Item> tag : tags) {
                ok |= stack.is(tag);
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasHazmatSuit(LivingEntity entity) {
        return wearsFullSuit(entity, HAZMAT);
    }

    public static boolean isImmuneToAll(LivingEntity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    // ---- applying it ----

    private static MobEffectInstance effect(int duration, RadiationIntensity intensity) {
        return new MobEffectInstance(ReactorEffects.RADIATION, duration, intensity.ordinal());
    }

    /** applyEffects: gives the effect if there is none already and the target is not shielded. Returns whether it did. */
    public static boolean applyEffects(LivingEntity entity, RadiationIntensity intensity) {
        if (!intensity.causesHarm()) {
            return false;
        }
        if (!entity.hasEffect(ReactorEffects.RADIATION)) {
            if (!isImmuneToAll(entity) && !intensity.hasSufficientShielding(entity)) {
                entity.addEffect(effect(intensity.potionDuration, intensity));
                return true;
            }
        }
        if (entity instanceof Creeper creeper) {
            creeper.getPersistentData().putBoolean("radioactive", true);
        }
        return false;
    }

    /** applyPulseEffects: a short dose (a second), from a neutron. */
    public static void applyPulseEffects(LivingEntity entity, RadiationIntensity intensity) {
        if (!entity.hasEffect(ReactorEffects.RADIATION) && !isImmuneToAll(entity) && !intensity.hasSufficientShielding(entity)) {
            entity.addEffect(effect(20, intensity));
        }
    }

    // ---- contamination ----

    /**
     * contaminateArea: scatters radiation entities around a point, enough to fill a ball of radius {@code range} at {@code density},
     * and returns the fraction of them that landed beyond {@code force} blocks of the middle. With {@code lineOfSight}, a spot is
     * kept only by a chance that shielding in the way reduces.
     */
    public static double contaminateArea(Level level, BlockPos center, int range, float density, double force, boolean lineOfSight,
                                         RadiationIntensity intensity) {
        double fraction = 1;
        int count = Math.max(1, (int) (Math.sqrt(range) * density));
        var random = level.random;
        for (int i = 0; i < count; i++) {
            BlockPos at = scatter(center, range, random);
            int tries = 0;
            while (lineOfSight && !isValidRadiationPosition(level, center, at, 2) && tries++ < 50) {
                at = scatter(center, range, random);
            }
            if (Math.sqrt(at.distSqr(center)) <= force) {
                fraction -= 1D / count;
            }
            if (!level.isClientSide) {
                RadiationEntity radiation = new RadiationEntity(level, range, intensity);
                radiation.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
                level.addFreshEntity(radiation);
            }
        }
        return fraction;
    }

    private static BlockPos scatter(BlockPos center, int range, net.minecraft.util.RandomSource random) {
        return center.offset(random.nextIntBetweenInclusive(-range, range), random.nextIntBetweenInclusive(-range, range),
                random.nextIntBetweenInclusive(-range, range));
    }

    /** isValidRadiationPosition: the chance radiation gets from the centre to the spot, past any shielding between. */
    private static boolean isValidRadiationPosition(Level level, BlockPos from, BlockPos to, double forceDistance) {
        if (Math.sqrt(from.distSqr(to)) <= forceDistance) {
            return true;
        }
        double chance = 1;
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 end = Vec3.atCenterOf(to);
        int steps = (int) Math.ceil(start.distanceTo(end));
        BlockPos last = null;
        for (int s = 1; s < steps; s++) {
            BlockPos at = BlockPos.containing(start.lerp(end, s / (double) steps));
            if (at.equals(last)) {
                continue;
            }
            last = at;
            RadiationShield shield = RadiationShield.of(level.getBlockState(at));
            if (shield != null) {
                chance *= 1 - shield.radiationDeflectChance / 100D;
            }
        }
        return chance > 0 && Chance.of(level.random, chance);
    }

    /** Whether there is a clear line between a point and an entity, for the waste storage's sickness. */
    public static boolean canSee(Level level, BlockPos from, LivingEntity target) {
        Vec3 start = Vec3.atCenterOf(from);
        return level.clip(new ClipContext(start, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target))
                .getType() == HitResult.Type.MISS;
    }

    /** A living thing within {@code range} of {@code pos} that can see it gets {@code intensity}. */
    public static void irradiateNearby(Level level, BlockPos pos, int range, RadiationIntensity intensity) {
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(range));
        for (LivingEntity e : near) {
            if (!intensity.hasSufficientShielding(e) && canSee(level, pos, e)) {
                applyEffects(e, intensity);
            }
        }
    }

    // ---- what it does to blocks ----

    /** transformBlock: under high-level radiation leaves fall, plants wither and grass dies; fluorite lights up. */
    public static void transformBlock(Level level, BlockPos pos, RadiationIntensity intensity, @Nullable java.util.UUID owner) {
        if (level.isClientSide || !(level instanceof ServerLevel)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(Blocks.DEAD_BUSH)) {
            return;
        }
        if (state.getBlock() instanceof FluoriteBlock fluorite) {
            fluorite.activate(level, pos, state);
            return;
        }
        if (state.getBlock() instanceof FluoriteOreBlock ore) {
            ore.activate(level, pos, state);
            return;
        }
        if (!intensity.isAtLeast(RadiationIntensity.HIGHLEVEL) || !ReactorConfig.RADIATION_TRANSFORMS_BLOCKS.get()
                || !WorldSafety.mayChange(level, pos, owner)) {
            return;
        }
        Block block = state.getBlock();
        if (state.is(BlockTags.LEAVES)) {
            level.removeBlock(pos, false);
        } else if (state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS) || state.is(BlockTags.REPLACEABLE_BY_TREES)
                && !state.is(Blocks.VINE) || block == Blocks.SUGAR_CANE || block == Blocks.CACTUS || block == Blocks.LILY_PAD
                || block == Blocks.VINE || block == Blocks.PUMPKIN || block == Blocks.MELON || block == Blocks.PUMPKIN_STEM
                || block == Blocks.MELON_STEM || block == Blocks.COCOA || block == Blocks.TALL_GRASS || block == Blocks.SHORT_GRASS
                || block == Blocks.FERN) {
            if (block == Blocks.TALL_GRASS || block == Blocks.SHORT_GRASS || block == Blocks.FERN || state.is(BlockTags.SAPLINGS)) {
                level.setBlockAndUpdate(pos, Blocks.DEAD_BUSH.defaultBlockState());
            } else {
                level.destroyBlock(pos, true);
            }
        } else if (block == Blocks.MOSSY_COBBLESTONE) {
            level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
        } else if (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM || block == Blocks.PODZOL) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        } else if (block == Blocks.INFESTED_STONE) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
    }

    public static Direction randomDirection(net.minecraft.util.RandomSource random) {
        return Direction.values()[random.nextInt(6)];
    }
}
