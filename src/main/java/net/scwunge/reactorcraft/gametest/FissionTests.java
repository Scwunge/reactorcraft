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
import net.scwunge.reactorcraft.content.machine.ControlRodBlockEntity;
import net.scwunge.reactorcraft.content.machine.CoolantCellBlockEntity;
import net.scwunge.reactorcraft.content.machine.FuelRodBlockEntity;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.core.CoolantState;
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

    private static NeutronEntity neutron(GameTestHelper helper, NeutronType type) {
        return new NeutronEntity(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0)), Direction.EAST, type);
    }

    private static void setTemperature(net.scwunge.reactorcraft.core.ReactorBlockEntity be, int temperature) throws ReflectiveOperationException {
        java.lang.reflect.Field field = net.scwunge.reactorcraft.core.ReactorBlockEntity.class.getDeclaredField("temperature");
        field.setAccessible(true);
        field.setInt(be, temperature);
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void neutronsCauseFissionInAFuelRod(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.FUEL_ROD.get());
        FuelRodBlockEntity rod = helper.getBlockEntity(at);
        rod.items().setStackInSlot(3, new ItemStack(ReactorItems.FUEL.get()));
        helper.runAfterDelay(3, () -> {
            int before = rod.getTemperature();
            helper.assertTrue(rod.isFissile() && !rod.isActive(), "fresh fuel is fissile and the core is idle");
            int absorbed = 0;
            for (int i = 0; i < 3000; i++) {
                if (rod.onNeutron(neutron(helper, NeutronType.FISSION), helper.getLevel(), helper.absolutePos(at))) {
                    absorbed++;
                }
            }
            helper.assertTrue(rod.isActive(), "a neutron wakes the core up");
            helper.assertTrue(absorbed > 20, "some of 3000 neutrons should have caused fission or poisoning, got " + absorbed);
            helper.assertTrue(rod.getTemperature() > before, "fission heats the core: " + before + " to " + rod.getTemperature());
            helper.assertTrue(rod.items().getStackInSlot(3).getDamageValue() > 0 || rod.items().getStackInSlot(3).isEmpty(),
                    "fission should have used up some fuel");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void fullWasteSoaksUpEveryNeutron(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.FUEL_ROD.get());
        FuelRodBlockEntity rod = helper.getBlockEntity(at);
        for (int i = 4; i < 12; i++) {
            rod.items().setStackInSlot(i, WasteManager.waste(net.scwunge.reactorcraft.content.waste.Isotope.CS137));
        }
        rod.items().setStackInSlot(3, new ItemStack(ReactorItems.FUEL.get()));
        for (int i = 0; i < 50; i++) {
            helper.assertTrue(rod.onNeutron(neutron(helper, NeutronType.FISSION), helper.getLevel(), helper.absolutePos(at))
                    || !NeutronType.FISSION.canTriggerFission(helper.getLevel().random), "full waste should absorb");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void controlRodsSoakUpNeutronsWhenLowered(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.CONTROL_ROD.get());
        ControlRodBlockEntity rod = helper.getBlockEntity(at);
        helper.assertTrue(rod.isActive(), "a new rod starts lowered");
        int absorbed = 0;
        for (int i = 0; i < 1000; i++) {
            if (rod.onNeutron(neutron(helper, NeutronType.FISSION), helper.getLevel(), helper.absolutePos(at))) {
                absorbed++;
            }
        }
        helper.assertTrue(absorbed > 500 && absorbed < 700, "60% of 1000, got " + absorbed);
        rod.toggle(false, false);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(!rod.isActive() && rod.rodPosition() == 20, "raised rod at " + rod.rodPosition());
            helper.assertTrue(!rod.onNeutron(neutron(helper, NeutronType.FISSION), helper.getLevel(), helper.absolutePos(at)), "a raised rod absorbs nothing");
            rod.drop(false);
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(rod.isActive(), "a SCRAM drops the rod all the way (" + rod.rodPosition() + ")");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void coolantCellsModerateAndPassTheirContentsDown(GameTestHelper helper) {
        BlockPos top = new BlockPos(2, 2, 2);
        BlockPos bottom = new BlockPos(2, 1, 2);
        helper.setBlock(top, ReactorBlocks.COOLANT_CELL.get());
        helper.setBlock(bottom, ReactorBlocks.COOLANT_CELL.get());
        CoolantCellBlockEntity upper = helper.getBlockEntity(top);
        CoolantCellBlockEntity lower = helper.getBlockEntity(bottom);
        upper.setCoolant(CoolantState.HEAVY);
        helper.assertTrue(helper.getBlockState(top).getValue(net.scwunge.reactorcraft.content.machine.CoolantCellBlock.COOLANT) == CoolantState.HEAVY,
                "the block shows its coolant");
        NeutronEntity fast = neutron(helper, NeutronType.FISSION);
        helper.assertTrue(!upper.onNeutron(fast, helper.getLevel(), helper.absolutePos(top)), "a coolant cell never absorbs");
        helper.assertTrue(fast.neutronSpeed() == NeutronType.NeutronSpeed.THERMAL, "heavy water moderates");
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(upper.coolant() == CoolantState.EMPTY && lower.coolant() == CoolantState.HEAVY,
                    "heavy water moved down: " + upper.coolant() + "/" + lower.coolant());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void fuelFeedsDownAColumnOfCores(GameTestHelper helper) {
        BlockPos bottom = new BlockPos(2, 1, 2);
        BlockPos top = new BlockPos(2, 2, 2);
        helper.setBlock(bottom, ReactorBlocks.FUEL_ROD.get());
        helper.setBlock(top, ReactorBlocks.FUEL_ROD.get());
        FuelRodBlockEntity lower = helper.getBlockEntity(bottom);
        FuelRodBlockEntity upper = helper.getBlockEntity(top);
        upper.items().setStackInSlot(3, new ItemStack(ReactorItems.FUEL.get()));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(upper.items().getStackInSlot(3).isEmpty() && upper.items().getStackInSlot(0).isEmpty(), "the upper core handed its fuel down");
            helper.assertTrue(lower.items().getStackInSlot(3).is(ReactorItems.FUEL.get()), "the lower core has the fuel in its reacting slot");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void theCpuFindsRodsAndScramsWithoutPower(GameTestHelper helper) {
        BlockPos cpuPos = new BlockPos(2, 1, 2);
        helper.setBlock(cpuPos, ReactorBlocks.CPU.get());
        helper.setBlock(cpuPos.east(), ReactorBlocks.CONTROL_ROD.get());
        helper.setBlock(cpuPos.south(), ReactorBlocks.CONTROL_ROD.get());
        helper.setBlock(cpuPos.south().south(), ReactorBlocks.CONTROL_ROD.get());
        net.scwunge.reactorcraft.content.machine.CpuBlockEntity cpu = helper.getBlockEntity(cpuPos);
        ControlRodBlockEntity east = helper.getBlockEntity(cpuPos.east());
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(cpu.layout().count() == 3, "the CPU should find 3 rods, found " + cpu.layout().count());
            helper.assertTrue(cpu.layout().minPower() == 3 * 1024, "needs 1024 W a rod");
            helper.assertTrue(cpu.onMenuButton(null, net.scwunge.reactorcraft.content.machine.CpuBlockEntity.BUTTON_RAISE_ALL), "raise all");
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(!east.isActive() || east.rodPosition() > -5, "raising all rods should have started");
            helper.assertTrue(cpu.onMenuButton(null, net.scwunge.reactorcraft.content.machine.CpuBlockEntity.toggleButton(1, 0, 0)), "toggle one rod");
            helper.assertTrue(!cpu.onMenuButton(null, net.scwunge.reactorcraft.content.machine.CpuBlockEntity.toggleButton(5, 0, 0)), "no rod there");
        });
        // there is no shaft power, so a few ticks after it has been up a while the CPU drops every rod
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(east.isActive() && east.rodPosition() == -5, "the unpowered CPU should have SCRAMmed: " + east.rodPosition());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void aBoilerFeedsSteamThroughALineToAGrate(GameTestHelper helper) {
        BlockPos boilerPos = new BlockPos(1, 1, 2);
        BlockPos linePos = new BlockPos(1, 2, 2);
        BlockPos linePos2 = new BlockPos(2, 2, 2);
        BlockPos gratePos = new BlockPos(3, 2, 2);
        // each block is placed after the line it should connect to, since setBlock only updates the neighbours' shapes
        helper.setBlock(linePos, ReactorBlocks.STEAM_LINE.get());
        helper.setBlock(linePos2, ReactorBlocks.STEAM_LINE.get());
        helper.setBlock(gratePos, ReactorBlocks.STEAM_GRATE.get());
        helper.setBlock(boilerPos, ReactorBlocks.REACTOR_BOILER.get());
        net.scwunge.reactorcraft.content.machine.ReactorBoilerBlockEntity boiler = helper.getBlockEntity(boilerPos);
        helper.assertTrue(helper.getBlockState(linePos).getValue(net.scwunge.reactorcraft.content.machine.SteamLineBlock.side(Direction.DOWN))
                && helper.getBlockState(linePos).getValue(net.scwunge.reactorcraft.content.machine.SteamLineBlock.side(Direction.EAST)),
                "the line should connect down to the boiler and east to the next line");
        helper.assertTrue(helper.getBlockState(linePos2).getValue(net.scwunge.reactorcraft.content.machine.SteamLineBlock.side(Direction.EAST)),
                "and on to the grate");
        helper.runAfterDelay(3, () -> {
            boiler.tank().setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3000));
            try {
                setTemperature(boiler, 400);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(boiler.tank().getFluidAmount() < 3000, "the boiler used water: " + boiler.tank().getFluidAmount());
            int steamBlocks = 0;
            for (int x = 0; x < 5; x++) {
                for (int y = 2; y < 60; y++) { // steam rises, so it may be far above by now
                    for (int z = 0; z < 5; z++) {
                        if (helper.getBlockState(new BlockPos(x, y, z)).is(ReactorBlocks.STEAM.get())) {
                            steamBlocks++;
                        }
                    }
                }
            }
            helper.assertTrue(steamBlocks > 0, "the grate should have let steam out above it");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void ammoniaAtSixHundredFiftyBlowsTheBoilerUp(GameTestHelper helper) {
        BlockPos boilerPos = new BlockPos(2, 1, 2);
        BlockPos linePos = new BlockPos(2, 2, 2);
        helper.setBlock(boilerPos, ReactorBlocks.REACTOR_BOILER.get());
        helper.setBlock(linePos, ReactorBlocks.STEAM_LINE.get());
        net.scwunge.reactorcraft.content.machine.ReactorBoilerBlockEntity boiler = helper.getBlockEntity(boilerPos);
        helper.runAfterDelay(3, () -> {
            boiler.tank().setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.reactorcraft.registry.ReactorFluids.AMMONIA.get(), 3000));
            try {
                setTemperature(boiler, 700);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(helper.getBlockState(boilerPos).isAir(), "the boiler should be gone");
            helper.assertTrue(helper.getBlockState(linePos).isAir(), "and the steam line above it");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void anOverheatedCoreMeltsDown(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.FUEL_ROD.get());
        FuelRodBlockEntity rod = helper.getBlockEntity(at);
        helper.runAfterDelay(3, () -> {
            try {
                setTemperature(rod, 2500);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(!helper.getBlockState(at).is(ReactorBlocks.FUEL_ROD.get()), "the core should be gone after a meltdown");
            helper.succeed();
        });
    }
}
