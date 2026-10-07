package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.SolarExchangerBlockEntity;
import net.scwunge.reactorcraft.content.machine.SolarTopBlockEntity;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.solar.SolarTowerBlockEntity;

/**
 * The sodium solar plant with RotaryCraft's real Solar Tower and Solar Mirrors: the two Sodium Cyclers on top of the tower tell it how hot they are and how many
 * mirrors shine, and the Heat Exchanger below it takes the sodium it heats.
 */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class SolarTowerTests {
    private static final String WIDE = "gametest/wide";
    private static final BlockPos EXCHANGER = new BlockPos(2, 1, 3);
    private static final BlockPos FOOT = EXCHANGER.above();
    private static final int MIRRORS = 8;

    private SolarTowerTests() {
    }

    private static Block rotary(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("rotarycraft:" + id));
    }

    /** A bedrock flywheel under the exchanger, already turning at the given torque and speed. */
    private static void spinUnder(GameTestHelper helper, int torque, int omega) {
        BlockPos at = EXCHANGER.below();
        helper.setBlock(at, rotary("flywheel_bedrock").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        CompoundTag tag = new CompoundTag();
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        helper.getBlockEntity(at).loadCustomOnly(tag, helper.getLevel().registryAccess());
    }

    /** A three block tower with eight mirrors in a row against its foot, two Sodium Cyclers on top and the exchanger under it, at noon. */
    private static void plant(GameTestHelper helper, int torque, int omega) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().setWeatherParameters(100000, 0, false, false);
        helper.setBlock(EXCHANGER, ReactorBlocks.SOLAR_EXCHANGER.get());
        for (int i = 0; i < 3; i++) {
            helper.setBlock(FOOT.above(i), rotary("solar_tower").defaultBlockState());
        }
        for (int i = 1; i <= MIRRORS; i++) {
            helper.setBlock(FOOT.east(i), rotary("solar_mirror").defaultBlockState());
        }
        helper.setBlock(FOOT.above(3), ReactorBlocks.SOLAR_TOP.get());
        helper.setBlock(FOOT.above(4), ReactorBlocks.SOLAR_TOP.get());
        spinUnder(helper, torque, omega);
    }

    @GameTest(template = WIDE, batch = "solar_plant_sodium", timeoutTicks = 200)
    public static void aTowerWithTwoCyclersHeatsSodiumIntoTheExchanger(GameTestHelper helper) {
        plant(helper, 64, 2048);
        SolarTopBlockEntity lower = helper.getBlockEntity(FOOT.above(3));
        SolarTopBlockEntity upper = helper.getBlockEntity(FOOT.above(4));
        SolarExchangerBlockEntity exchanger = helper.getBlockEntity(EXCHANGER);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(lower.isActive() && !upper.isActive(), "only the lower cycler of the pair works");
            helper.assertTrue(exchanger.isActive(), "the exchanger has 131 kW at 2048 rad/s under it");
            // the sunlight raises the cycler, but slowly: start it hot, as a long day in a big plant would
            lower.addTemperature(900 - lower.getTemperature());
            for (int i = 0; i < 3; i++) {
                SolarTowerBlockEntity tower = helper.getBlockEntity(FOOT.above(i));
                tower.tank().fill(new FluidStack(ReactorFluids.SODIUM.get(), SolarTowerBlockEntity.TANK), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        helper.succeedWhen(() -> {
            SolarTowerBlockEntity top = helper.getBlockEntity(FOOT.above(2));
            helper.assertTrue(top.temperature() >= 800, "the tower should read the cycler's heat: " + top.temperature());
            helper.assertTrue(exchanger.sodiumTank().getFluidAmount() > 0, "no sodium came down to the exchanger (tank " + exchanger.sodiumTank().getFluidAmount()
                    + ", tower temperature " + top.temperature() + ")");
            helper.assertTrue(exchanger.sodiumTank().getFluid().is(ReactorFluids.WARM_SODIUM.get()), "it should be warm sodium");
        });
    }

    @GameTest(template = WIDE, batch = "solar_plant_cold", timeoutTicks = 100)
    public static void anExchangerWithoutPowerTakesNoSodium(GameTestHelper helper) {
        plant(helper, 1, 1);
        SolarTopBlockEntity lower = helper.getBlockEntity(FOOT.above(3));
        SolarExchangerBlockEntity exchanger = helper.getBlockEntity(EXCHANGER);
        helper.runAfterDelay(5, () -> {
            lower.addTemperature(900 - lower.getTemperature());
            for (int i = 0; i < 3; i++) {
                SolarTowerBlockEntity tower = helper.getBlockEntity(FOOT.above(i));
                tower.tank().fill(new FluidStack(ReactorFluids.SODIUM.get(), SolarTowerBlockEntity.TANK), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        helper.runAfterDelay(60, () -> {
            helper.assertFalse(exchanger.isActive(), "one watt is not enough");
            helper.assertTrue(exchanger.sodiumTank().getFluidAmount() == 0, "sodium reached an exchanger with no power");
            helper.succeed();
        });
    }
}
