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
}
