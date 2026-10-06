package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.content.machine.AbsorberBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReflectorBlockEntity;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

/** Milestone 6: the other reactor types and their parts. */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class CoreVariantTests {
    private static final String EMPTY = MaterialTests.EMPTY;

    private CoreVariantTests() {
    }

    private static NeutronEntity neutron(GameTestHelper helper, NeutronType type) {
        return new NeutronEntity(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0)), Direction.EAST, type);
    }

    @GameTest(template = EMPTY)
    public static void aReflectorSendsNeutronsBackAndSoaksUpSome(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.REFLECTOR.get());
        ReflectorBlockEntity reflector = helper.getBlockEntity(at);
        int absorbed = 0;
        int reversed = 0;
        for (int i = 0; i < 1000; i++) {
            NeutronEntity n = neutron(helper, NeutronType.FISSION);
            double before = n.getDeltaMovement().x;
            boolean soaked = reflector.onNeutron(n, helper.getLevel(), helper.absolutePos(at));
            helper.assertTrue(n.neutronSpeed() == NeutronType.NeutronSpeed.THERMAL, "reflected neutrons are thermal");
            if (soaked) {
                absorbed++;
            }
            if (n.getDeltaMovement().x == -before) {
                reversed++;
            }
        }
        helper.assertTrue(absorbed > 300 && absorbed < 450, "about 3/8 absorbed, got " + absorbed);
        helper.assertTrue(reversed > 200 && reversed < 300, "about 1/4 reversed, got " + reversed);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aBreederTurnsBreederFuelIntoPlutonium(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.BREEDER_CORE.get());
        net.scwunge.reactorcraft.content.machine.BreederCoreBlockEntity breeder = helper.getBlockEntity(at);
        breeder.items().setStackInSlot(3, new net.minecraft.world.item.ItemStack(net.scwunge.reactorcraft.registry.ReactorItems.BREEDER_FUEL.get()));
        helper.assertTrue(breeder.isFissile(), "breeder fuel makes it fissile");
        helper.assertTrue(!breeder.isItemValid(0, new net.minecraft.world.item.ItemStack(net.scwunge.reactorcraft.registry.ReactorItems.FUEL.get())), "ordinary fuel is refused");
        boolean plutonium = false;
        for (int i = 0; i < 40000 && !plutonium; i++) {
            breeder.onNeutron(neutron(helper, NeutronType.BREEDER), helper.getLevel(), helper.absolutePos(at));
            for (int s = 0; s < breeder.items().getSlots(); s++) {
                net.minecraft.world.item.ItemStack in = breeder.items().getStackInSlot(s);
                plutonium |= in.is(net.scwunge.reactorcraft.registry.ReactorItems.PLUTONIUM.get());
                if (net.scwunge.reactorcraft.content.waste.WasteManager.isWaste(in)) {
                    breeder.items().setStackInSlot(s, net.minecraft.world.item.ItemStack.EMPTY); // waste poisons the core, so empty it as a player would
                }
            }
        }
        helper.assertTrue(plutonium, "twenty conversions should have made a plutonium pellet");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aSodiumHeaterMakesHotSodiumAndCools(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.SODIUM_HEATER.get());
        net.scwunge.reactorcraft.content.machine.SodiumHeaterBlockEntity heater = helper.getBlockEntity(at);
        helper.runAfterDelay(3, () -> {
            heater.tank().setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.reactorcraft.registry.ReactorFluids.SODIUM.get(), 2000));
            heater.setTemperature(600);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!heater.outputTank().isEmpty() && heater.outputTank().getFluid().is(net.scwunge.reactorcraft.registry.ReactorFluids.HOT_SODIUM.get()),
                    "hot sodium collects: " + heater.outputTank().getFluid());
            helper.assertTrue(heater.tank().getFluidAmount() < 2000 && heater.getTemperature() < 600, "it used sodium and cooled: "
                    + heater.tank().getFluidAmount() + " mB, " + heater.getTemperature() + " C");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void aThoriumCoreBurnsLiquidFuelAndMakesLiquidWaste(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.THORIUM_CORE.get());
        net.scwunge.reactorcraft.content.machine.ThoriumCoreBlockEntity core = helper.getBlockEntity(at);
        helper.assertTrue(!core.isFissile(), "it makes no neutrons of its own");
        core.fuelTank().setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.reactorcraft.registry.ReactorFluids.LIFBE_FUEL.get(), 4000));
        helper.runAfterDelay(3, () -> {
            core.setTemperature(800);
            for (int i = 0; i < 3000; i++) {
                core.onNeutron(neutron(helper, NeutronType.FISSION), helper.getLevel(), helper.absolutePos(at));
                core.setTemperature(800);
                if (core.fuelTank().getFluidAmount() < 1000) {
                    core.fuelTank().setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.reactorcraft.registry.ReactorFluids.LIFBE_FUEL.get(), 4000));
                }
            }
            helper.assertTrue(core.fuelTank().getFluidAmount() < 4000 || !core.hotFuelTank().isEmpty(), "fuel was burned");
            helper.assertTrue(!core.hotFuelTank().isEmpty() && core.hotFuelTank().getFluid().is(net.scwunge.reactorcraft.registry.ReactorFluids.HOT_LIFBE.get()),
                    "into hot fuel salt");
            helper.assertTrue(!core.wasteTank().isEmpty(), "leaving some liquid waste");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aFuelDumpPoursAnOverheatedCoresFuelOnTheGround(GameTestHelper helper) {
        BlockPos dump = new BlockPos(2, 2, 2);
        BlockPos coreAt = new BlockPos(2, 3, 2);
        helper.setBlock(dump, ReactorBlocks.FUEL_DUMP.get());
        helper.setBlock(coreAt, ReactorBlocks.THORIUM_CORE.get());
        net.scwunge.reactorcraft.content.machine.ThoriumCoreBlockEntity core = helper.getBlockEntity(coreAt);
        core.fuelTank().setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.reactorcraft.registry.ReactorFluids.LIFBE_FUEL.get(), 1000));
        helper.runAfterDelay(3, () -> core.setTemperature(1150));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(core.fuelTank().getFluidAmount() < 1000, "the core's fuel was drained: " + core.fuelTank().getFluidAmount());
            net.minecraft.world.level.block.state.BlockState pool = helper.getBlockState(new BlockPos(2, 1, 2));
            helper.assertTrue(pool.is(ReactorBlocks.THORIUM_FUEL.get()), "and poured out below as spilled fuel: " + pool);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aPebbleBedRunsCyclesAndCountsItsCluster(GameTestHelper helper) throws ReflectiveOperationException {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ReactorBlocks.PEBBLE_BED.get());
        helper.setBlock(new BlockPos(2, 1, 1), ReactorBlocks.PEBBLE_BED.get());
        helper.setBlock(new BlockPos(4, 1, 1), ReactorBlocks.PEBBLE_BED.get());
        net.scwunge.reactorcraft.content.machine.PebbleBedBlockEntity bed = helper.getBlockEntity(at);
        helper.assertTrue(bed.isItemValid(0, new net.minecraft.world.item.ItemStack(net.scwunge.reactorcraft.registry.ReactorItems.TRISO_PELLET.get())), "pellets go in");
        helper.assertTrue(!bed.isItemValid(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK)), "nothing else does");
        bed.items().setStackInSlot(46, new net.minecraft.world.item.ItemStack(net.scwunge.reactorcraft.registry.ReactorItems.TRISO_PELLET.get()));
        java.lang.reflect.Method cycle = net.scwunge.reactorcraft.content.machine.PebbleBedBlockEntity.class.getDeclaredMethod("runDecayCycle");
        cycle.setAccessible(true);
        helper.runAfterDelay(3, () -> {
            try {
                int before = bed.getTemperature();
                for (int i = 0; i < 400; i++) {
                    cycle.invoke(bed);
                }
                helper.assertTrue(bed.getTemperature() == before + 20 * 400, "every cycle heats 20 degrees");
                net.minecraft.world.item.ItemStack left = bed.items().getStackInSlot(46);
                helper.assertTrue(net.scwunge.reactorcraft.core.FuelStage.get(left) > 0 || !left.is(net.scwunge.reactorcraft.registry.ReactorItems.TRISO_PELLET.get()), "and wears the pellet");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            helper.assertTrue(bed.clusterSize() == 3, "beds within three blocks of each other form one cluster: " + bed.clusterSize());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aCarbonDioxideHeaterMakesHotCarbonDioxide(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.CO2_HEATER.get());
        net.scwunge.reactorcraft.content.machine.Co2HeaterBlockEntity heater = helper.getBlockEntity(at);
        helper.runAfterDelay(3, () -> {
            heater.tank().setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.reactorcraft.registry.ReactorFluids.CO2.get(), 2000));
            heater.setTemperature(900);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!heater.outputTank().isEmpty() && heater.outputTank().getFluid().is(net.scwunge.reactorcraft.registry.ReactorFluids.HOT_CO2.get()),
                    "hot CO2: " + heater.outputTank().getFluid());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void anAbsorberHeatsUpOnFusionNeutronsOnly(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.ABSORBER.get());
        AbsorberBlockEntity absorber = helper.getBlockEntity(at);
        helper.runAfterDelay(3, () -> {
            int before = absorber.getTemperature();
            helper.assertTrue(!absorber.onNeutron(neutron(helper, NeutronType.FISSION), helper.getLevel(), helper.absolutePos(at)), "it ignores fission neutrons");
            helper.assertTrue(absorber.onNeutron(neutron(helper, NeutronType.FUSION), helper.getLevel(), helper.absolutePos(at)), "and soaks up fusion neutrons");
            helper.assertTrue(absorber.getTemperature() == before + 40, "for 40 degrees each");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void heatPipesCarryHeatFromAReactorToABoiler(GameTestHelper helper) {
        BlockPos bed = new BlockPos(1, 1, 1);
        BlockPos boiler = new BlockPos(4, 1, 1);
        helper.setBlock(bed, ReactorBlocks.PEBBLE_BED.get());
        helper.setBlock(new BlockPos(2, 1, 1), ReactorBlocks.HEAT_PIPE.get());
        helper.setBlock(new BlockPos(3, 1, 1), ReactorBlocks.HEAT_PIPE.get());
        helper.setBlock(boiler, ReactorBlocks.REACTOR_BOILER.get());
        net.scwunge.reactorcraft.content.machine.PebbleBedBlockEntity source = helper.getBlockEntity(bed);
        net.scwunge.reactorcraft.content.machine.ReactorBoilerBlockEntity target = helper.getBlockEntity(boiler);
        helper.runAfterDelay(3, () -> source.setTemperature(1500));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(target.getTemperature() > 200, "the boiler warms: " + target.getTemperature());
            helper.assertTrue(source.getTemperature() < 1500, "the reactor cools: " + source.getTemperature());
            helper.assertTrue(target.getReactorType() == net.scwunge.reactorcraft.core.ReactorType.HTGR, "and knows the heat came from a pebble bed: " + target.getReactorType());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void aHeatExchangerOnlyTakesHotFluidsAndNeedsPower(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.HEAT_EXCHANGER.get());
        net.scwunge.reactorcraft.content.machine.HeatExchangerBlockEntity exchanger = helper.getBlockEntity(at);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(exchanger.inputTank().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 500),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0, "plain water does not go in");
            helper.assertTrue(exchanger.inputTank().fill(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.reactorcraft.registry.ReactorFluids.HOT_SODIUM.get(), 500),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 500, "hot sodium does");
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(exchanger.outputTank().isEmpty(), "with no shaft power it cools nothing");
            helper.assertTrue(exchanger.currentRecipe() == net.scwunge.reactorcraft.content.machine.HeatExchangerBlockEntity.Exchange.SODIUM, "but it knows the recipe");
            helper.succeed();
        });
    }
}
