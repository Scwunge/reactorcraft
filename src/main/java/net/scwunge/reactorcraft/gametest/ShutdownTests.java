package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.ControlRodBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;
import net.scwunge.reactorcraft.content.machine.TurbineCoreBlockEntity;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

/**
 * A server that is stopping unloads its chunks, and a block entity that asks for a block in a neighbouring chunk while its own chunk is unloaded makes the server wait for
 * that chunk for ever (the server spun at full speed and never exited). These tests make a block entity leave the world next to chunks that are not loaded and check that it
 * does not load them to look.
 */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public class ShutdownTests {
    /** The last block of a chunk, far from anything loaded. */
    private static final BlockPos EDGE = new BlockPos(2_000_015, 70, 2_000_015);

    @GameTest(template = MaterialTests.EMPTY, timeoutTicks = 40, batch = "shutdown")
    public static void aTurbineCoreLeavingTheWorldLoadsNoChunkToLookForItsNeighbour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertFalse(level.hasChunkAt(EDGE.east()) || level.hasChunkAt(EDGE.west()), "the chunks beside the test spot are loaded already");
        TurbineCoreBlockEntity turbine = new TurbineCoreBlockEntity(EDGE, ReactorBlocks.TURBINE_CORE.get().defaultBlockState().setValue(ReactorMachineBlock.LOOK, Direction.EAST));
        turbine.setLevel(level);
        Fluid lube = BuiltInRegistries.FLUID.get(ResourceLocation.parse("rotarycraft:lubricant"));
        turbine.lubricantTank().setFluid(new FluidStack(lube, 5000));
        turbine.setRemoved();
        helper.assertFalse(level.hasChunkAt(EDGE.east()) || level.hasChunkAt(EDGE.west()), "a turbine core loaded a chunk when it was removed");
        helper.succeed();
    }

    @GameTest(template = MaterialTests.EMPTY, timeoutTicks = 40, batch = "shutdown")
    public static void aLinkedControlRodLeavingTheWorldLoadsNoChunkToLookForItsCpu(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos cpu = EDGE.offset(64, 0, 0);
        helper.assertFalse(level.hasChunkAt(cpu), "the chunk of the CPU is loaded already");
        ControlRodBlockEntity rod = new ControlRodBlockEntity(EDGE, ReactorBlocks.CONTROL_ROD.get().defaultBlockState());
        rod.setLevel(level);
        rod.link(cpu);
        rod.setRemoved();
        helper.assertFalse(level.hasChunkAt(cpu), "a control rod loaded a chunk when it was removed");
        helper.succeed();
    }

    /** The check the two tests above make only means something if looking at a block does load its chunk: it does. */
    @GameTest(template = MaterialTests.EMPTY, timeoutTicks = 40, batch = "shutdown")
    public static void lookingAtABlockInAnUnloadedChunkLoadsIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos far = EDGE.offset(0, 0, 160);
        helper.assertFalse(level.hasChunkAt(far), "the chunk is loaded already");
        level.getBlockEntity(far);
        helper.assertTrue(level.hasChunkAt(far), "a look at a block did not load its chunk, so the tests above prove nothing");
        helper.succeed();
    }
}
