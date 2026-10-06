package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.BigTurbineBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorFlywheelBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorGeneratorBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;
import net.scwunge.reactorcraft.content.machine.SolenoidBlock;
import net.scwunge.reactorcraft.content.machine.SteamInjectorBlockEntity;
import net.scwunge.reactorcraft.content.multi.MultiPartBlock;
import net.scwunge.reactorcraft.content.multi.MultiStructure;
import net.scwunge.reactorcraft.content.multi.PowerStructures;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.ReactorTypeMix;
import net.scwunge.reactorcraft.core.WorkingFluid;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

/** Milestone 8: the big turbine, the flywheel and the generator. */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class PowerTests {
    private static final String EMPTY = MaterialTests.EMPTY;

    private PowerTests() {
    }

    private static Fluid lubricant() {
        return BuiltInRegistries.FLUID.get(ResourceLocation.parse("rotarycraft:lubricant"));
    }

    /** Builds a big turbine of {@code stages} turbine blocks at {@code origin} (the first, where steam goes in), the steam moving along +x unless turned. */
    private static void buildTurbine(GameTestHelper helper, BlockPos origin, int stages, int rotation, boolean leaveOutOnePart) {
        Level level = helper.getLevel();
        PowerStructures.TurbineStructure structure = PowerStructures.TURBINE;
        Direction forward = MultiStructure.forward(rotation);
        boolean skipped = !leaveOutOnePart;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < structure.cells().size(); i++) {
                MultiStructure.Cell cell = structure.cells().get(i);
                if (structure.stageOf(i) >= stages) {
                    continue;
                }
                Block block = cell.sample().get();
                boolean isPart = block instanceof MultiPartBlock;
                if (isPart != (pass == 1)) {
                    continue;
                }
                BlockPos pos = MultiStructure.at(origin, cell.x(), cell.y(), cell.z(), rotation);
                if (isPart && !skipped) {
                    skipped = true;
                    continue;
                }
                BlockState state = block.defaultBlockState();
                if (state.hasProperty(ReactorMachineBlock.LOOK)) {
                    state = state.setValue(ReactorMachineBlock.LOOK, forward);
                }
                level.setBlock(pos, state, 3);
            }
        }
    }

    private static boolean turbineFormed(GameTestHelper helper, BlockPos origin, int stages, int rotation) {
        for (int k = 0; k < stages; k++) {
            if (!(helper.getLevel().getBlockEntity(MultiStructure.at(origin, k, 0, 0, rotation)) instanceof BigTurbineBlockEntity turbine) || !turbine.isFormed()) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void aBigTurbineFormsWithAnyNumberOfStagesAndBreaksWithAPart(GameTestHelper helper) {
        int row = 0;
        for (int stages : new int[]{1, 3, 7}) {
            BlockPos origin = helper.absolutePos(new BlockPos(8, 8, 8 + 16 * row));
            buildTurbine(helper, origin, stages, 0, false);
            helper.assertTrue(turbineFormed(helper, origin, stages, 0), stages + " turbine blocks in their casing should form a turbine");
            // casing blocks beyond the last stage are not part of it
            row++;
        }
        BlockPos origin = helper.absolutePos(new BlockPos(8, 8, 8));
        helper.getLevel().setBlock(MultiStructure.at(origin, 0, 1, 1, 0), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(!turbineFormed(helper, origin, 1, 0), "taking a casing block out unforms the turbine");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aBigTurbineWithAMissingPartDoesNotForm(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(8, 8, 8));
        buildTurbine(helper, origin, 2, 0, true);
        helper.assertTrue(!turbineFormed(helper, origin, 2, 0), "one casing block is missing");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aBigTurbineFormsFacingAnyWay(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(8, 8, 8));
        buildTurbine(helper, origin, 2, 1, false);
        helper.assertTrue(turbineFormed(helper, origin, 2, 1), "the same casing, turned a quarter");
        helper.succeed();
    }

    /** Steam in the line behind the first stage, lubricant in an injector, and the turbine spins and gives shaft power out of its far end. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void steamFromALineSpinsABigTurbine(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(8, 8, 8));
        buildTurbine(helper, origin, 2, 0, false);
        Level level = helper.getLevel();
        BlockPos linePos = MultiStructure.at(origin, -1, 0, 0, 0);
        level.setBlock(linePos, ReactorBlocks.STEAM_LINE.get().defaultBlockState(), 3);
        net.scwunge.reactorcraft.content.machine.SteamLineBlockEntity line = (net.scwunge.reactorcraft.content.machine.SteamLineBlockEntity) level.getBlockEntity(linePos);
        BlockPos injectorPos = MultiStructure.at(origin, -1, 1, 0, 0);
        SteamInjectorBlockEntity injector = (SteamInjectorBlockEntity) level.getBlockEntity(injectorPos);
        helper.assertTrue(injector != null, "the casing's injector has a block entity");
        injector.fluidHandler(null).fill(new FluidStack(lubricant(), 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        BigTurbineBlockEntity first = (BigTurbineBlockEntity) level.getBlockEntity(origin);
        BigTurbineBlockEntity second = (BigTurbineBlockEntity) level.getBlockEntity(MultiStructure.at(origin, 1, 0, 0, 0));
        helper.onEachTick(() -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Steam", 3000);
            tag.putInt("Working", WorkingFluid.WATER.ordinal());
            CompoundTag mix = new CompoundTag();
            mix.putDouble(ReactorType.FISSION.name(), 3000);
            tag.put("Sources", mix);
            line.loadCustomOnly(tag, level.registryAccess());
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(first.stage() == 0 && second.stage() == 1 && first.totalStages() == 2, "two stages: " + first.stage() + "/" + second.stage());
            helper.assertTrue(first.lubricantTank().getFluidAmount() > 0, "the first stage drew lubricant from the injector");
            helper.assertTrue(first.omega() > 1000, "spinning: " + first.omega());
            helper.assertTrue(second.omega() > 1000, "the second stage follows: " + second.omega());
            helper.assertTrue(second.getTorqueOut(Direction.EAST) > 0 && second.getOmegaOut(Direction.EAST) > 0, "power out of the far end");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void aBigTurbineDoesNothingWithoutItsCasing(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ReactorBlocks.BIG_TURBINE.get().defaultBlockState().setValue(ReactorMachineBlock.LOOK, Direction.EAST));
        BigTurbineBlockEntity turbine = helper.getBlockEntity(at);
        turbine.lubricantTank().setFluid(new FluidStack(lubricant(), 5000));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(turbine.omega() == 0 && turbine.totalStages() == 0 && !turbine.isFormed(), "no casing, no turbine");
            helper.succeed();
        });
    }

    /** The flywheel's disc forms round it (whichever way it faces) and it passes on the power of the turbine beside it, out of its back. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void aFlywheelPassesTheTurbinePowerOnOnceItsCasingStands(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(8, 8, 8));
        Level level = helper.getLevel();
        // a flywheel facing east, in a disc across the way it faces; its turbine is east of it
        for (MultiStructure.Cell cell : PowerStructures.FLYWHEEL.cells()) {
            Block block = cell.sample().get();
            if (!(block instanceof MultiPartBlock)) {
                BlockState state = block.defaultBlockState();
                level.setBlock(MultiStructure.at(origin, cell.x(), cell.y(), cell.z(), 0), state.hasProperty(ReactorMachineBlock.LOOK)
                        ? state.setValue(ReactorMachineBlock.LOOK, Direction.EAST) : state, 3);
            }
        }
        level.setBlock(origin.east(), ReactorBlocks.TURBINE_CORE.get().defaultBlockState().setValue(ReactorMachineBlock.LOOK, Direction.EAST), 3);
        ReactorFlywheelBlockEntity flywheel = (ReactorFlywheelBlockEntity) level.getBlockEntity(origin);
        net.scwunge.reactorcraft.content.machine.TurbineCoreBlockEntity turbine = (net.scwunge.reactorcraft.content.machine.TurbineCoreBlockEntity) level.getBlockEntity(origin.east());
        CompoundTag tag = new CompoundTag();
        tag.putInt("Omega", 4000);
        tag.putInt("Steam", 100);
        turbine.loadCustomOnly(tag, level.registryAccess());
        helper.runAfterDelay(10, () -> helper.assertTrue(flywheel.getTorqueOut(Direction.WEST) == 0 && !flywheel.isFormed(), "no disc, no power out"));
        helper.runAfterDelay(12, () -> {
            for (MultiStructure.Cell cell : PowerStructures.FLYWHEEL.cells()) {
                Block block = cell.sample().get();
                if (block instanceof MultiPartBlock) {
                    level.setBlock(MultiStructure.at(origin, cell.x(), cell.y(), cell.z(), 0), block.defaultBlockState(), 3);
                }
            }
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(flywheel.isFormed(), "the disc is complete");
            helper.assertTrue(level.getBlockState(origin).getValue(SolenoidBlock.FORMED), "and the block shows it");
            helper.assertTrue(flywheel.omega() > 0 && flywheel.getOmegaOut(Direction.WEST) > 0, "it turns with the turbine: " + flywheel.omega());
            helper.assertTrue(flywheel.getOmegaOut(Direction.EAST) == 0, "and gives nothing out of its front");
            helper.succeed();
        });
    }

    /** A generator ten blocks from a turbine, in its housing, makes power from the turbine's. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void aGeneratorTakesTheTurbinePowerWhileItsHousingStands(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(8, 8, 8));
        Level level = helper.getLevel();
        for (int pass = 0; pass < 2; pass++) {
            for (MultiStructure.Cell cell : PowerStructures.GENERATOR.cells()) {
                Block block = cell.sample().get();
                if ((block instanceof MultiPartBlock) != (pass == 1)) {
                    continue;
                }
                BlockState state = block.defaultBlockState();
                level.setBlock(MultiStructure.at(origin, cell.x(), cell.y(), cell.z(), 0), state.hasProperty(ReactorMachineBlock.LOOK)
                        ? state.setValue(ReactorMachineBlock.LOOK, Direction.EAST) : state, 3);
            }
        }
        BlockPos turbinePos = origin.east(ReactorGeneratorBlockEntity.LENGTH);
        level.setBlock(turbinePos, ReactorBlocks.TURBINE_CORE.get().defaultBlockState().setValue(ReactorMachineBlock.LOOK, Direction.WEST), 3);
        ReactorGeneratorBlockEntity generator = (ReactorGeneratorBlockEntity) level.getBlockEntity(origin);
        net.scwunge.reactorcraft.content.machine.TurbineCoreBlockEntity turbine = (net.scwunge.reactorcraft.content.machine.TurbineCoreBlockEntity) level.getBlockEntity(turbinePos);
        helper.onEachTick(() -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Omega", 800);
            tag.putInt("Steam", 200);
            turbine.loadCustomOnly(tag, level.registryAccess());
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(generator.isFormed(), "ten blocks of housing round the generator form it");
            helper.assertTrue(generator.power() > 0 && generator.generatedUnits() > 0, "it makes power: " + generator.power());
        });
        helper.runAfterDelay(65, () -> {
            level.setBlock(MultiStructure.at(origin, 4, 2, 0, 0), Blocks.AIR.defaultBlockState(), 3);
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(!generator.isFormed(), "a housing block gone, the generator is off");
            helper.assertTrue(generator.power() == 0, "and makes nothing: " + generator.power());
            helper.succeed();
        });
    }

    /** A turbine with a formed casing that is spinning fast explodes when the casing is broken. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void breakingTheCasingOfAFastTurbineWreckstIt(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(8, 8, 8));
        buildTurbine(helper, origin, 1, 0, false);
        Level level = helper.getLevel();
        BigTurbineBlockEntity turbine = (BigTurbineBlockEntity) level.getBlockEntity(origin);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Omega", 5000);
        tag.putBoolean("Formed", true);
        turbine.loadCustomOnly(tag, level.registryAccess());
        helper.runAfterDelay(5, () -> level.setBlock(MultiStructure.at(origin, 0, 1, 1, 0), Blocks.AIR.defaultBlockState(), 3));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!(level.getBlockState(origin).getBlock() == ReactorBlocks.BIG_TURBINE.get()), "the turbine was wrecked");
            helper.succeed();
        });
    }
}
