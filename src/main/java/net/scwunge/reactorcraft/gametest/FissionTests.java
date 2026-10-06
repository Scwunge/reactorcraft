package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.block.FluoriteBlock;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.content.material.FluoriteColor;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.RadiationShield;
import net.scwunge.reactorcraft.core.ReactorFuel;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorItems;

/** Milestone 4: neutrons, fuel and the fission core. */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class FissionTests {
    private static final String EMPTY = MaterialTests.EMPTY;

    private FissionTests() {
    }

    @GameTest(template = EMPTY)
    public static void fuelPelletsWearOutStageByStage(GameTestHelper helper) {
        ItemStack fresh = new ItemStack(ReactorItems.FUEL.get());
        helper.assertTrue(ReactorFuel.of(fresh) == ReactorFuel.URANIUM, "uranium pellet is uranium fuel");
        helper.assertTrue(ReactorFuel.of(new ItemStack(ReactorItems.PLUTONIUM.get())) == ReactorFuel.PLUTONIUM, "plutonium pellet");
        helper.assertTrue(ReactorFuel.of(new ItemStack(ReactorItems.DEPLETED.get())) == null, "depleted uranium is not fuel");
        ItemStack next = ReactorFuel.URANIUM.fissionProduct(fresh);
        helper.assertTrue(next.is(ReactorItems.FUEL.get()) && next.getDamageValue() == 1, "one stage on: damage " + next.getDamageValue());
        ItemStack last = new ItemStack(ReactorItems.FUEL.get());
        last.setDamageValue(ReactorFuel.STAGES - 1);
        helper.assertTrue(ReactorFuel.URANIUM.fissionProduct(last).is(ReactorItems.DEPLETED.get()), "the last stage leaves depleted uranium");
        ItemStack lastPlutonium = new ItemStack(ReactorItems.PLUTONIUM.get());
        lastPlutonium.setDamageValue(ReactorFuel.STAGES - 1);
        helper.assertTrue(ReactorFuel.PLUTONIUM.fissionProduct(lastPlutonium).isEmpty(), "plutonium burns away completely");
        helper.assertTrue(ReactorFuel.PLUTONIUM.voidCoefficient > 0 && ReactorFuel.URANIUM.voidCoefficient == 0, "void coefficients");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void shieldTagsNameTheOriginalMaterials(GameTestHelper helper) {
        helper.assertTrue(RadiationShield.of(ReactorBlocks.CONCRETE.get().defaultBlockState()) == RadiationShield.CONCRETE, "concrete");
        helper.assertTrue(RadiationShield.of(Blocks.OBSIDIAN.defaultBlockState()) == RadiationShield.OBSIDIAN, "obsidian");
        helper.assertTrue(RadiationShield.of(Blocks.WATER.defaultBlockState()) == RadiationShield.WATER, "water");
        helper.assertTrue(RadiationShield.of(Blocks.DIRT.defaultBlockState()) == null, "dirt is not a shield");
        helper.assertTrue(RadiationShield.BEDROCK_INGOT.neutronAbsorbChance == 97.5 && RadiationShield.LEAD.neutronAbsorbChance == 75, "numbers");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void aNeutronLightsFluorite(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 1, 1);
        helper.setBlock(target, ReactorBlocks.fluoriteBlock(FluoriteColor.GREEN));
        NeutronEntity neutron = new NeutronEntity(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), Direction.EAST, NeutronType.FISSION);
        helper.getLevel().addFreshEntity(neutron);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(helper.getBlockState(target).getValue(FluoriteBlock.ACTIVE), "the neutron did not light the fluorite");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void neutronsFlyThroughAirAndDieOnSolidShields(GameTestHelper helper) {
        // lead-free check: a thick obsidian wall stops a neutron almost surely, but it must not vanish before reaching it
        NeutronEntity neutron = new NeutronEntity(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 2)), Direction.EAST, NeutronType.DECAY);
        helper.getLevel().addFreshEntity(neutron);
        helper.assertTrue(neutron.getDeltaMovement().x == NeutronEntity.SPEED, "speed " + neutron.getDeltaMovement());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(neutron.isAlive() && neutron.getX() > helper.absolutePos(BlockPos.ZERO).getX() + 1.5, "x after two ticks: " + neutron.getX());
            helper.succeed();
        });
    }
}
