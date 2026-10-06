package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.ElectrolyzerBlockEntity;
import net.scwunge.reactorcraft.content.machine.FluidSynthesizerBlockEntity;
import net.scwunge.reactorcraft.content.machine.GasCollectorBlockEntity;
import net.scwunge.reactorcraft.content.machine.IsotopeCentrifugeBlockEntity;
import net.scwunge.reactorcraft.content.machine.UraniumProcessorBlockEntity;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.reactorcraft.registry.ReactorItems;

/** Milestone 3: the processing machines. */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class MachineTests {
    private static final String EMPTY = MaterialTests.EMPTY;
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private MachineTests() {
    }

    private static void fill(IFluidHandler handler, net.minecraft.world.level.material.Fluid fluid, int amount) {
        handler.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    @GameTest(template = EMPTY)
    public static void centrifugeSpeedsUpWithTheShaft(GameTestHelper helper) {
        helper.assertTrue(IsotopeCentrifugeBlockEntity.cycleTime(262144) == 900, "minimum speed cycle");
        helper.assertTrue(IsotopeCentrifugeBlockEntity.cycleTime(1048576) == 600, "1 Mrad/s cycle");
        helper.assertTrue(IsotopeCentrifugeBlockEntity.cycleTime(67108864) == 8, "top speed cycle");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void processorMakesUf6InTwoStages(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.URANIUM_PROCESSOR.get());
        UraniumProcessorBlockEntity be = helper.getBlockEntity(AT);
        // like the original, stage two only runs while the input tank still holds fluid
        fill(be.inputTank(), Fluids.WATER, 1000);
        be.items().setStackInSlot(UraniumProcessorBlockEntity.SLOT_FLUORITE, new ItemStack(ReactorItems.fluorite(
                net.scwunge.reactorcraft.content.material.FluoriteColor.GREEN)));
        be.items().setStackInSlot(UraniumProcessorBlockEntity.SLOT_MAIN, new ItemStack(ReactorItems.URANIUM_INGOT.get()));
        helper.runAfterDelay(520, () -> {
            helper.assertTrue(be.outputTank().getFluidAmount() == 1000
                            && be.outputTank().getFluid().is(ReactorFluids.URANIUM_HEXAFLUORIDE.get()),
                    "expected 1000 mB of UF6, got " + be.outputTank().getFluid() + " (in " + be.inputTank().getFluid()
                            + ", mid " + be.intermediateTank().getFluid() + ", process " + be.process() + ", fluorite "
                            + be.items().getStackInSlot(0) + ", main " + be.items().getStackInSlot(2) + ")");
            helper.assertTrue(be.inputTank().getFluidAmount() == 750, "one fluorite should use 250 mB of water: " + be.inputTank().getFluid());
            helper.assertTrue(be.intermediateTank().getFluidAmount() == 125, "HF left over: " + be.intermediateTank().getFluidAmount());
            helper.assertTrue(be.items().getStackInSlot(UraniumProcessorBlockEntity.SLOT_MAIN).isEmpty()
                    && be.items().getStackInSlot(UraniumProcessorBlockEntity.SLOT_FLUORITE).isEmpty(), "ingredients were not consumed");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void processorRefusesWrongInputs(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.URANIUM_PROCESSOR.get());
        UraniumProcessorBlockEntity be = helper.getBlockEntity(AT);
        helper.assertTrue(be.inputTank().fill(new FluidStack(ReactorFluids.SODIUM.get(), 100), IFluidHandler.FluidAction.SIMULATE) == 0,
                "the input tank took sodium");
        helper.assertTrue(!be.isItemValid(UraniumProcessorBlockEntity.SLOT_MAIN, new ItemStack(Items.DIRT)), "dirt is not a uranium source");
        helper.assertTrue(be.isItemValid(UraniumProcessorBlockEntity.SLOT_MAIN, new ItemStack(ReactorItems.URANIUM_INGOT.get())), "uranium ingot");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void electrolyzerSplitsHeavyWaterWhenShocked(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.ELECTROLYZER.get());
        ElectrolyzerBlockEntity be = helper.getBlockEntity(AT);
        fill(be.inputTank(), ReactorFluids.HEAVY_WATER.get(), 100);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(be.recipe() == ElectrolyzerBlockEntity.Electrolysis.HEAVY_WATER, "no recipe picked: " + be.recipe());
            helper.assertTrue(be.lightTank().isEmpty(), "ran without being shocked");
            // 4096 + 65536 gives sqrt(65536) / 16 = 16 ticks a shock
            for (int i = 0; i < 4; i++) {
                be.onDischarge(ElectrolyzerBlockEntity.MIN_DISCHARGE + 65536, 4);
            }
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(be.lightTank().getFluidAmount() == 100 && be.lightTank().getFluid().is(ReactorFluids.DEUTERIUM.get()),
                    "deuterium: " + be.lightTank().getFluid());
            helper.assertTrue(be.heavyTank().getFluidAmount() == 50 && be.heavyTank().getFluid().is(ReactorFluids.OXYGEN.get()),
                    "oxygen: " + be.heavyTank().getFluid());
            helper.assertTrue(be.inputTank().isEmpty(), "heavy water left: " + be.inputTank().getFluid());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void electrolyzerIgnoresSmallShocksWithoutAJob(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.ELECTROLYZER.get());
        ElectrolyzerBlockEntity be = helper.getBlockEntity(AT);
        be.onDischarge(ElectrolyzerBlockEntity.MIN_DISCHARGE, 1);
        helper.assertTrue(be.timer().getTick() == 0, "a shock advanced an idle electrolyzer");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void synthesizerMakesAmmoniaWhenHot(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.FLUID_SYNTHESIZER.get());
        FluidSynthesizerBlockEntity be = helper.getBlockEntity(AT);
        be.items().setStackInSlot(FluidSynthesizerBlockEntity.SLOT_BUCKET, new ItemStack(Items.WATER_BUCKET));
        be.items().setStackInSlot(FluidSynthesizerBlockEntity.SLOT_A, new ItemStack(ReactorItems.QUICKLIME.get()));
        be.items().setStackInSlot(FluidSynthesizerBlockEntity.SLOT_B, new ItemStack(ReactorItems.AMMONIUM_CHLORIDE.get()));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(be.waterTank().getFluidAmount() == 1000, "the bucket was not emptied: " + be.waterTank().getFluidAmount());
            helper.assertTrue(be.items().getStackInSlot(FluidSynthesizerBlockEntity.SLOT_BUCKET).is(Items.BUCKET), "no empty bucket left");
            helper.assertTrue(be.productTank().isEmpty(), "made ammonia while cold");
            be.addTemperature(400 - be.getTemperature());
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(be.productTank().getFluidAmount() == 1000 && be.productTank().getFluid().is(ReactorFluids.AMMONIA.get()),
                    "ammonia: " + be.productTank().getFluid());
            helper.assertTrue(be.waterTank().getFluidAmount() == 750, "water left: " + be.waterTank().getFluidAmount());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void collectorTakesCo2FromABurningFurnace(GameTestHelper helper) {
        BlockPos furnacePos = AT.relative(Direction.SOUTH);
        helper.setBlock(furnacePos, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.LIT, true));
        helper.setBlock(AT, ReactorBlocks.GAS_COLLECTOR.get()); // looks south by default
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos);
        furnace.setItem(1, new ItemStack(Items.COAL));
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = furnace.saveWithoutMetadata(registries);
        tag.putShort("lit_time", (short) 400);
        tag.putShort("lit_duration", (short) 400);
        furnace.loadWithComponents(tag, registries);
        GasCollectorBlockEntity be = helper.getBlockEntity(AT);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(be.tank().getFluidAmount() >= 10 && be.tank().getFluid().is(ReactorFluids.CO2.get()),
                    "no CO2 collected: " + be.tank().getFluid());
            IFluidHandler back = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.NORTH);
            IFluidHandler front = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.SOUTH);
            helper.assertTrue(back != null && front == null, "CO2 should come out of the back only");
            helper.assertTrue(back.drain(10, IFluidHandler.FluidAction.SIMULATE).getAmount() == 10, "could not drain CO2 from the back");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void machinesExposeFluidHandlers(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.ELECTROLYZER.get());
        BlockPos abs = helper.absolutePos(AT);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.UP) != null, "no handler on top");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.DOWN) != null, "no handler below");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.NORTH) != null, "no handler on the side");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void electrolyzerSplitsSaltWhenHot(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.ELECTROLYZER.get());
        ElectrolyzerBlockEntity be = helper.getBlockEntity(AT);
        be.items().setStackInSlot(0, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("rotarycraft:salt")), 2));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(be.recipe() == null, "salt ran while cold: " + be.recipe());
            be.addTemperature(900 - be.getTemperature());
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(be.recipe() == ElectrolyzerBlockEntity.Electrolysis.SALT, "no salt recipe: " + be.recipe());
            for (int i = 0; i < 4; i++) {
                be.onDischarge(ElectrolyzerBlockEntity.MIN_DISCHARGE + 65536, 4);
            }
        });
        helper.runAfterDelay(9, () -> {
            helper.assertTrue(be.lightTank().getFluidAmount() == 100 && be.lightTank().getFluid().is(ReactorFluids.CHLORINE.get()),
                    "chlorine: " + be.lightTank().getFluid());
            helper.assertTrue(be.heavyTank().getFluidAmount() == 100 && be.heavyTank().getFluid().is(ReactorFluids.SODIUM.get()),
                    "sodium: " + be.heavyTank().getFluid());
            helper.assertTrue(be.items().getStackInSlot(0).getCount() == 1, "one salt should be used: " + be.items().getStackInSlot(0));
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void overheatedSynthesizerBlowsUp(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.FLUID_SYNTHESIZER.get());
        FluidSynthesizerBlockEntity be = helper.getBlockEntity(AT);
        helper.runAfterDelay(3, () -> be.addTemperature(2000));
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(helper.getBlockState(AT).isAir(), "an overheated synthesizer should be gone");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void refrigeratorCyclesGiveLiquidOxygen(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.GAS_COLLECTOR.get());
        GasCollectorBlockEntity be = helper.getBlockEntity(AT);
        be.onCompleteCycle(70);
        helper.assertTrue(be.tank().getFluidAmount() == 20 && be.tank().getFluid().is(ReactorFluids.LIQUID_OXYGEN.get()),
                "liquid oxygen: " + be.tank().getFluid());
        helper.succeed();
    }
}
