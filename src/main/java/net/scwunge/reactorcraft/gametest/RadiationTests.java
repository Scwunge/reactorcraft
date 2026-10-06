package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.entity.RadiationEntity;
import net.scwunge.reactorcraft.core.RadiationEffects;
import net.scwunge.reactorcraft.core.RadiationIntensity;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorEffects;
import net.scwunge.reactorcraft.registry.ReactorItems;

/** Milestone 5: radiation. */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class RadiationTests {
    private static final String EMPTY = MaterialTests.EMPTY;

    private RadiationTests() {
    }

    private static void wearHazmat(LivingEntity entity) {
        entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ReactorItems.HAZMAT_HELMET.get()));
        entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ReactorItems.HAZMAT_CHESTPLATE.get()));
        entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ReactorItems.HAZMAT_LEGGINGS.get()));
        entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(ReactorItems.HAZMAT_BOOTS.get()));
    }

    @GameTest(template = EMPTY)
    public static void radiationSicknessHitsTheUnprotected(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        helper.assertTrue(RadiationEffects.applyEffects(zombie, RadiationIntensity.MODERATE), "an unprotected zombie takes it");
        helper.assertTrue(zombie.hasEffect(ReactorEffects.RADIATION)
                && zombie.getEffect(ReactorEffects.RADIATION).getAmplifier() == RadiationIntensity.MODERATE.ordinal(), "at the intensity's level");
        helper.assertTrue(!RadiationEffects.applyEffects(zombie, RadiationIntensity.LOWLEVEL), "but only once at a time");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void hazmatStopsHighLevelButNotLethalRadiation(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        wearHazmat(zombie);
        helper.assertTrue(RadiationEffects.hasHazmatSuit(zombie), "a full suit is a hazmat suit");
        helper.assertTrue(!RadiationEffects.applyEffects(zombie, RadiationIntensity.HIGHLEVEL) && !zombie.hasEffect(ReactorEffects.RADIATION),
                "high-level radiation is stopped");
        helper.assertTrue(!RadiationEffects.applyEffects(zombie, RadiationIntensity.MODERATE), "and moderate");
        helper.assertTrue(RadiationEffects.applyEffects(zombie, RadiationIntensity.LETHAL), "a meltdown gets through");
        Zombie partial = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        partial.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ReactorItems.HAZMAT_HELMET.get()));
        helper.assertTrue(RadiationEffects.applyEffects(partial, RadiationIntensity.HIGHLEVEL), "half a suit is not enough");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void radiationPatchesIrradiateAndWashAway(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        RadiationEntity patch = new RadiationEntity(helper.getLevel(), 3, RadiationIntensity.MODERATE);
        patch.setPos(helper.absolutePos(new BlockPos(2, 1, 2)).getCenter());
        helper.getLevel().addFreshEntity(patch);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(zombie.hasEffect(ReactorEffects.RADIATION), "a zombie standing in it should be irradiated");
            patch.clean();
            patch.clean();
            patch.clean();
            patch.clean();
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(!patch.isAlive(), "washed away to nothing");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void contaminationSpreadsRadiationAroundAPoint(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(2, 1, 2));
        double fraction = RadiationEffects.contaminateArea(helper.getLevel(), center, 4, 4, 1.5, false, RadiationIntensity.HIGHLEVEL);
        helper.runAfterDelay(2, () -> {
            int count = helper.getLevel().getEntitiesOfClass(RadiationEntity.class, new net.minecraft.world.phys.AABB(center).inflate(6)).size();
            helper.assertTrue(count >= 8, "sqrt(4) * 4 = 8 patches expected, found " + count);
            helper.assertTrue(fraction <= 1 && fraction >= 0, "fraction " + fraction);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void shieldingBlocksReduceContaminationReach(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.OBSIDIAN);
        helper.assertTrue(net.scwunge.reactorcraft.core.RadiationShield.of(helper.getBlockState(new BlockPos(2, 1, 2)))
                == net.scwunge.reactorcraft.core.RadiationShield.OBSIDIAN, "obsidian shields");
        helper.assertTrue(ReactorBlocks.CONCRETE.get().defaultBlockState().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                net.minecraft.resources.ResourceLocation.parse("reactorcraft:shield/concrete"))), "concrete is in the shield tag");
        helper.succeed();
    }
}
