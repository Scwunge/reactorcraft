package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.block.FluoriteBlock;
import net.scwunge.reactorcraft.content.material.FluoriteColor;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.reactorcraft.registry.ReactorItems;

import java.util.List;

@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class MaterialTests {
    public static final String EMPTY = "gametest/empty";

    private MaterialTests() {
    }

    @GameTest(template = EMPTY)
    public static void canisterFillsAndEmpties(GameTestHelper helper) {
        IFluidHandlerItem can = new ItemStack(ReactorItems.EMPTY_CANISTER.get()).getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(can != null, "empty canister has no fluid handler");
        int filled = can.fill(new FluidStack(ReactorFluids.URANIUM_HEXAFLUORIDE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000 && can.getContainer().is(ReactorItems.UF6_CANISTER.get()), "filling did not give a UF6 canister");
        helper.assertTrue(can.fill(new FluidStack(ReactorFluids.HEAVY_WATER.get(), 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
                "a full canister took more fluid");
        FluidStack out = can.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(out.getAmount() == 1000 && out.getFluid() == ReactorFluids.URANIUM_HEXAFLUORIDE.get(), "draining gave " + out);
        helper.assertTrue(can.getContainer().is(ReactorItems.EMPTY_CANISTER.get()), "draining did not leave an empty canister");
        helper.assertTrue(can.fill(new FluidStack(ReactorFluids.HEAVY_WATER.get(), 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
                "a canister took heavy water, which only goes in buckets");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void heavyWaterBucketWorksBothWays(GameTestHelper helper) {
        IFluidHandlerItem bucket = new ItemStack(Items.BUCKET).getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(bucket != null && bucket.fill(new FluidStack(ReactorFluids.HEAVY_WATER.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "an empty bucket would not take heavy water");
        helper.assertTrue(bucket.getContainer().is(ReactorItems.HEAVY_WATER_BUCKET.get()), "filled bucket is " + bucket.getContainer());
        IFluidHandlerItem full = new ItemStack(ReactorItems.HEAVY_WATER_BUCKET.get()).getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(full.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000 && full.getContainer().is(Items.BUCKET),
                "draining the heavy water bucket did not give back a bucket");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void fluoriteGlowsAfterANeutron(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        FluoriteBlock block = ReactorBlocks.fluoriteBlock(FluoriteColor.GREEN);
        helper.setBlock(pos, block);
        BlockPos abs = helper.absolutePos(pos);
        block.activate(helper.getLevel(), abs, helper.getLevel().getBlockState(abs));
        helper.assertTrue(helper.getBlockState(pos).getValue(FluoriteBlock.ACTIVE), "fluorite did not light up");
        helper.assertTrue(helper.getBlockState(pos).getLightEmission() == 15, "lit fluorite should be full brightness");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!helper.getBlockState(pos).getValue(FluoriteBlock.ACTIVE), "fluorite stayed lit");
            helper.succeed();
        });
    }

    private static List<ItemStack> drops(GameTestHelper helper, Block block) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = block.defaultBlockState();
        return Block.getDrops(state, helper.getLevel(), pos, null, null, new ItemStack(Items.IRON_PICKAXE));
    }

    @GameTest(template = EMPTY)
    public static void oresDropWhatTheOriginalDid(GameTestHelper helper) {
        helper.assertTrue(drops(helper, ReactorBlocks.PITCHBLENDE_ORE.get()).getFirst().is(ReactorBlocks.PITCHBLENDE_ORE.get().asItem()),
                "pitchblende should drop itself, to be smelted");
        helper.assertTrue(drops(helper, ReactorBlocks.CALCITE_ORE.get()).getFirst().is(ReactorItems.CALCITE_CRYSTAL.get()), "calcite ore drop");
        helper.assertTrue(drops(helper, ReactorBlocks.MAGNETITE_ORE.get()).getFirst().is(ReactorItems.LODESTONE.get()), "magnetite ore drop");
        List<ItemStack> ammonium = drops(helper, ReactorBlocks.AMMONIUM_CHLORIDE_ORE.get());
        helper.assertTrue(ammonium.stream().anyMatch(s -> s.is(ReactorItems.AMMONIUM_CHLORIDE.get()))
                && ammonium.stream().anyMatch(s -> s.is(Items.NETHERRACK)), "ammonium chloride ore should drop ammonium chloride and netherrack");
        List<ItemStack> fluorite = drops(helper, ReactorBlocks.fluoriteOre(FluoriteColor.RED));
        int count = fluorite.stream().filter(s -> s.is(ReactorItems.fluorite(FluoriteColor.RED))).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(count >= 1 && count <= 5, "fluorite ore dropped " + count + " crystals");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void oreFeaturesAreRegistered(GameTestHelper helper) {
        var placed = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        for (String ore : List.of("pitchblende_ore", "cadmium_ore", "indium_ore", "silver_ore", "endblende_ore", "ammonium_chloride_ore",
                "calcite_ore", "magnetite_ore", "thorite_ore", "fluorite_ore")) {
            helper.assertTrue(placed.containsKey(ReactorCraft.id(ore)), "no placed feature for " + ore);
        }
        helper.succeed();
    }
}
