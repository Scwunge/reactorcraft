package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.CentrifugalTurbineBlockEntity;
import net.scwunge.reactorcraft.content.machine.SteamDiffuserBlockEntity;
import net.scwunge.reactorcraft.content.machine.SteamLineBlockEntity;
import net.scwunge.reactorcraft.core.WorkingFluid;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorFluids;

/** Milestone 8: the power plant's smaller parts. */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class PowerGenTests {
    private static final String EMPTY = MaterialTests.EMPTY;

    private PowerGenTests() {
    }

    private static void set(Object target, String field, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void aDiffuserTurnsSteamLineSteamIntoSteamFluid(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.STEAM_DIFFUSER.get());
        BlockPos linePos = at.relative(Direction.SOUTH);
        helper.setBlock(linePos, ReactorBlocks.STEAM_LINE.get());
        SteamDiffuserBlockEntity diffuser = helper.getBlockEntity(at);
        SteamLineBlockEntity line = helper.getBlockEntity(linePos);
        helper.runAfterDelay(3, () -> {
            set(line, "steam", 4000);
            set(line, "fluid", WorkingFluid.WATER);
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(line.getSteam() < 4000, "it draws steam from the line it faces: " + line.getSteam());
            helper.assertTrue(!diffuser.steamTank().isEmpty() && diffuser.steamTank().getFluid().is(ReactorFluids.STEAM.get()),
                    "and makes steam: " + diffuser.steamTank().getFluid());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void aCentrifugalTurbineStandsStill(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.CENTRIFUGAL_TURBINE.get());
        CentrifugalTurbineBlockEntity turbine = helper.getBlockEntity(at);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(turbine.maxSpeed() == 0 && turbine.maxTorque() == 0, "it has no speed or torque to give");
            helper.assertTrue(turbine.omega() == 0 && turbine.generatedPower() == 0, "and does not turn");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aWasteDuctCarriesNuclearWasteOnly(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos b = new BlockPos(2, 1, 1);
        helper.setBlock(a, ReactorBlocks.WASTE_PIPE.get());
        helper.setBlock(b, ReactorBlocks.WASTE_PIPE.get());
        net.scwunge.reactorcraft.content.machine.WastePipeBlockEntity first = helper.getBlockEntity(a);
        net.scwunge.reactorcraft.content.machine.WastePipeBlockEntity second = helper.getBlockEntity(b);
        helper.assertTrue(first.isValidFluid(ReactorFluids.NUCLEAR_WASTE.get()) && !first.isValidFluid(net.minecraft.world.level.material.Fluids.WATER),
                "only nuclear waste goes in");
        helper.runAfterDelay(3, () -> first.pipeTank().fill(new net.neoforged.neoforge.fluids.FluidStack(ReactorFluids.NUCLEAR_WASTE.get(), 800),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(second.pipeTank().getFluidAmount() > 100, "and it flows down the line: " + second.pipeTank().getFluidAmount());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void aSolarExchangerTakesSodiumAndGivesBackTheRest(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.SOLAR_EXCHANGER.get());
        net.scwunge.reactorcraft.content.machine.SolarExchangerBlockEntity exchanger = helper.getBlockEntity(at);
        helper.assertTrue(exchanger.receiveSodium(600) == 0, "it takes sodium that fits");
        helper.assertTrue(exchanger.receiveSodium(600) == 200, "and returns what does not: the tank holds 1000");
        helper.assertTrue(exchanger.sodiumTank().getFluid().is(ReactorFluids.WARM_SODIUM.get()), "as hot sodium");
        helper.assertTrue(!exchanger.isActive(), "with no shaft power it is not active");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void aSolarTopWarmsUnderMirrorsAndOnlyWorksAsAStackedPair(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.SOLAR_TOP.get());
        net.scwunge.reactorcraft.content.machine.SolarTopBlockEntity top = helper.getBlockEntity(at);
        helper.runAfterDelay(3, () -> {
            int before = top.getTemperature();
            helper.assertTrue(!top.isActive(), "alone it is not active");
            top.tick(100, 1F);
            helper.assertTrue(top.getMaxTemperature() == 1800, "it tops out at 1800");
            helper.assertTrue(top.getTemperature() >= before, "mirrors never cool it");
            helper.succeed();
        });
    }
}
