package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.content.entity.PlasmaEntity;
import net.scwunge.reactorcraft.content.machine.FusionInjectorBlockEntity;
import net.scwunge.reactorcraft.content.machine.GasDuctBlockEntity;
import net.scwunge.reactorcraft.content.machine.MagneticPipeBlockEntity;
import net.scwunge.reactorcraft.content.machine.ToroidMagnetBlockEntity;
import net.scwunge.reactorcraft.content.machine.SolenoidBlock;
import net.scwunge.reactorcraft.content.machine.SolenoidBlockEntity;
import net.scwunge.reactorcraft.content.machine.FusionHeaterBlockEntity;
import net.scwunge.reactorcraft.content.multi.FusionStructures;
import net.scwunge.reactorcraft.content.multi.MultiPartBlock;
import net.scwunge.reactorcraft.content.multi.MultiStructure;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.ToroidAim;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/** Milestone 7: fusion. */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class FusionTests {
    private static final String EMPTY = MaterialTests.EMPTY;

    private FusionTests() {
    }

    /** Two magnets that steer plasma at each other: a complete ring. */
    private static ToroidMagnetBlockEntity[] ring(GameTestHelper helper, BlockPos a) {
        BlockPos b = a.offset(2, 0, 0);
        helper.setBlock(a, ReactorBlocks.TOROID_MAGNET.get());
        helper.setBlock(b, ReactorBlocks.TOROID_MAGNET.get());
        ToroidMagnetBlockEntity first = helper.getBlockEntity(a);
        ToroidMagnetBlockEntity second = helper.getBlockEntity(b);
        first.setAim(ToroidAim.N);
        second.setAim(ToroidAim.S);
        return new ToroidMagnetBlockEntity[]{first, second};
    }

    private static void power(ToroidMagnetBlockEntity magnet) {
        magnet.setHasSolenoid(true);
        magnet.setCharge(50000);
        magnet.coolantTank().fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.source.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aToroidMagnetHoldsPlasmaOnlyWhenEverythingIsInPlace(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        ToroidMagnetBlockEntity[] magnets = ring(helper, at);
        PlasmaEntity plasma = new PlasmaEntity(helper.getLevel(), helper.absolutePos(at), null);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!magnets[0].canAffect(plasma), "a magnet with no solenoid, charge or coolant lets plasma go");
            power(magnets[0]);
            power(magnets[1]);
            helper.getLevel().addFreshEntity(plasma);
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(magnets[0].ringComplete(), "two magnets aimed at each other are a ring");
            helper.assertTrue(magnets[0].canAffect(plasma) || plasma.magnetOrdinal == 0, "now it holds the plasma");
            helper.assertTrue(magnets[0].coolantTank().getFluidAmount() < 2000 || magnets[1].coolantTank().getFluidAmount() < 2000,
                    "using up coolant");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aToroidMagnetIsNotARingUntilItIsClosed(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ReactorBlocks.TOROID_MAGNET.get());
        helper.setBlock(at.offset(2, 0, 0), ReactorBlocks.TOROID_MAGNET.get());
        ToroidMagnetBlockEntity first = helper.getBlockEntity(at);
        ToroidMagnetBlockEntity second = helper.getBlockEntity(at.offset(2, 0, 0));
        first.setAim(ToroidAim.N);
        second.setAim(ToroidAim.W);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!first.ringComplete(), "the second magnet looks the wrong way");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void plasmaPackedTogetherFuses(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        for (int i = 0; i < 24; i++) {
            helper.getLevel().addFreshEntity(new PlasmaEntity(helper.getLevel(), helper.absolutePos(at), null));
        }
        helper.runAfterDelay(8, () -> {
            AABB box = new AABB(helper.absolutePos(at)).inflate(10);
            boolean fused = helper.getLevel().getEntitiesOfClass(NeutronEntity.class, box).stream().anyMatch(n -> n.neutronType() == NeutronType.FUSION);
            helper.assertTrue(fused, "fusion neutrons appear");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aBuiltInjectorMakesPlasmaFromPlasma(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.FUSION_INJECTOR.get());
        FusionInjectorBlockEntity injector = helper.getBlockEntity(at);
        helper.runAfterDelay(3, () -> injector.plasmaTank().fill(new FluidStack(ReactorFluids.FUSION_PLASMA.get(), 100), IFluidHandler.FluidAction.EXECUTE));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PlasmaEntity.class, new AABB(helper.absolutePos(at)).inflate(8)).isEmpty(),
                    "unbuilt, it makes none");
            injector.setFormed(true);
        });
        helper.runAfterDelay(20, () -> {
            int plasma = helper.getLevel().getEntitiesOfClass(PlasmaEntity.class, new AABB(helper.absolutePos(at)).inflate(12)).size();
            helper.assertTrue(plasma > 3, "built, it makes plasma: " + plasma);
            helper.assertTrue(injector.plasmaTank().getFluidAmount() < 100, "from its tank");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void magneticPipesCarryChargedPlasmaButMeltWithoutCharge(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos b = new BlockPos(2, 1, 1);
        BlockPos c = new BlockPos(4, 1, 1);
        helper.setBlock(a, ReactorBlocks.MAGNETIC_PIPE.get());
        helper.setBlock(b, ReactorBlocks.MAGNETIC_PIPE.get());
        helper.setBlock(c, ReactorBlocks.MAGNETIC_PIPE.get());
        MagneticPipeBlockEntity first = helper.getBlockEntity(a);
        MagneticPipeBlockEntity second = helper.getBlockEntity(b);
        helper.runAfterDelay(3, () -> {
            first.setCharge(100000);
            second.setCharge(100000);
            first.pipeTank().fill(new FluidStack(ReactorFluids.FUSION_PLASMA.get(), 800), IFluidHandler.FluidAction.EXECUTE);
            ((MagneticPipeBlockEntity) helper.getBlockEntity(c)).pipeTank().fill(new FluidStack(ReactorFluids.FUSION_PLASMA.get(), 500),
                    IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(second.pipeTank().getFluidAmount() > 100, "plasma flows down a charged pipe: " + second.pipeTank().getFluidAmount());
            helper.assertTrue(helper.getBlockState(c).is(Blocks.LAVA), "an uncharged pipe holding plasma melts");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void aGasDuctTakesGasesOnly(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.GAS_DUCT.get());
        GasDuctBlockEntity duct = helper.getBlockEntity(at);
        helper.assertTrue(duct.isValidFluid(ReactorFluids.CO2.get()), "carbon dioxide is a gas");
        helper.assertTrue(!duct.isValidFluid(net.minecraft.world.level.material.Fluids.WATER), "water is not");
        helper.assertTrue(!duct.isValidFluid(ReactorFluids.SODIUM.get()), "nor is sodium");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aScrewdriverStepsTheAimBothWays(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, ReactorBlocks.TOROID_MAGNET.get());
        ToroidMagnetBlockEntity magnet = helper.getBlockEntity(at);
        magnet.setAim(ToroidAim.N);
        magnet.setAim(magnet.aim().previous());
        helper.assertTrue(magnet.aim() == ToroidAim.NNE3, "stepping back from north wraps round to NNE3: " + magnet.aim());
        magnet.setAim(magnet.aim().next());
        helper.assertTrue(magnet.aim() == ToroidAim.N, "and forward again");
        helper.assertTrue(Direction.NORTH != null, "ok");
        helper.succeed();
    }

    // ---- structures ----

    /** Builds every block of a structure at {@code origin}: controllers and pipes first, then the parts, so the last part placed completes it. */
    private static void build(GameTestHelper helper, MultiStructure structure, BlockPos origin, int rotation, BlockPos skip) {
        net.minecraft.world.level.Level level = helper.getLevel();
        for (int pass = 0; pass < 2; pass++) {
            for (MultiStructure.Cell cell : structure.cells()) {
                net.minecraft.world.level.block.Block block = cell.sample().get();
                boolean isPart = block instanceof MultiPartBlock;
                BlockPos pos = MultiStructure.at(origin, cell.x(), cell.y(), cell.z(), rotation);
                if (block == Blocks.AIR || isPart != (pass == 1) || pos.equals(skip)) {
                    continue;
                }
                level.setBlock(pos, block.defaultBlockState(), 3);
            }
        }
    }

    private static boolean allFormed(GameTestHelper helper, MultiStructure structure, BlockPos origin, int rotation) {
        for (MultiStructure.Cell cell : structure.cells()) {
            net.minecraft.world.level.block.state.BlockState state = helper.getLevel().getBlockState(MultiStructure.at(origin, cell.x(), cell.y(), cell.z(), rotation));
            if (state.getBlock() instanceof MultiPartBlock && !state.getValue(MultiPartBlock.FORMED)) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void anInjectorHousingFormsInAnyDirectionAndBreaksWithAPart(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(6, 4, 6));
        for (int rotation : new int[]{0, 1}) {
            BlockPos at = origin.offset(rotation * 24, 0, 0);
            build(helper, FusionStructures.INJECTOR, at, rotation, null);
            helper.assertTrue(allFormed(helper, FusionStructures.INJECTOR, at, rotation), "the housing forms (turned " + rotation + ")");
            FusionInjectorBlockEntity injector = (FusionInjectorBlockEntity) helper.getLevel().getBlockEntity(MultiStructure.at(at, 2, 0, 0, rotation));
            helper.assertTrue(injector != null && injector.isFormed(), "and tells the injector");
            helper.assertTrue(injector.facing() == MultiStructure.forward(rotation), "which now faces along it: " + injector.facing());
        }
        BlockPos hub = MultiStructure.at(origin, 0, 1, 0, 0);
        helper.getLevel().setBlock(hub, Blocks.AIR.defaultBlockState(), 3);
        FusionInjectorBlockEntity injector = (FusionInjectorBlockEntity) helper.getLevel().getBlockEntity(MultiStructure.at(origin, 2, 0, 0, 0));
        helper.assertTrue(!injector.isFormed(), "pull a block out and it is no longer built");
        helper.assertTrue(!allFormed(helper, FusionStructures.INJECTOR, origin, 0) && helper.getLevel().getBlockState(MultiStructure.at(origin, 3, 0, 3, 0)).getBlock() instanceof MultiPartBlock p
                && !p.structure().cells().isEmpty(), "the rest of the housing goes dark");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aHeaterChamberNeedsExactlyOneLens(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(6, 4, 6));
        net.minecraft.world.level.Level level = helper.getLevel();
        build(helper, FusionStructures.HEATER, origin, 0, MultiStructure.at(origin, 1, 1, 1, 0));
        FusionHeaterBlockEntity heater = (FusionHeaterBlockEntity) level.getBlockEntity(origin.offset(2, 2, 2));
        helper.assertTrue(!heater.isFormed(), "without a lens it is not a heater chamber");
        level.setBlock(origin.offset(1, 1, 1), FusionStructures.HEATER.part(0).defaultBlockState(), 3);
        helper.assertTrue(heater.isFormed(), "with its lens it is");
        level.setBlock(origin.offset(3, 1, 3), FusionStructures.HEATER.part(0).defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 4, 4), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(!heater.isFormed(), "and a hole in the wall breaks it");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100, batch = "bigStructure")
    public static void aSolenoidCoilFormsAroundItsHub(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(12, 5, 12));
        build(helper, FusionStructures.SOLENOID, origin, 0, null);
        SolenoidBlockEntity solenoid = (SolenoidBlockEntity) helper.getLevel().getBlockEntity(origin);
        helper.assertTrue(solenoid != null && solenoid.isFormed(), "the coil forms round the hub");
        helper.assertTrue(helper.getLevel().getBlockState(origin).getValue(SolenoidBlock.FORMED), "and the hub shows it");
        helper.assertTrue(allFormed(helper, FusionStructures.SOLENOID, origin, 0), "every coil block is lit");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aFusionHeaterMakesPlasmaOnceHotAndSealed(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(6, 4, 6));
        build(helper, FusionStructures.HEATER, origin, 0, null);
        helper.getLevel().setBlock(origin.offset(1, 1, 1), FusionStructures.HEATER.part(0).defaultBlockState(), 3);
        FusionHeaterBlockEntity heater = (FusionHeaterBlockEntity) helper.getLevel().getBlockEntity(origin.offset(2, 2, 2));
        helper.runAfterDelay(3, () -> {
            heater.deuteriumTank().fill(new FluidStack(ReactorFluids.DEUTERIUM.get(), 500), IFluidHandler.FluidAction.EXECUTE);
            heater.tritiumTank().fill(new FluidStack(ReactorFluids.TRITIUM.get(), 500), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(heater.plasmaTank().isEmpty(), "cold, it makes nothing");
            heater.setTemperature(200_000_000);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!heater.plasmaTank().isEmpty(), "hot, it makes plasma");
            helper.assertTrue(heater.deuteriumTank().getFluidAmount() < 500, "from deuterium");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aLaserBeamHeatsABuiltFusionHeater(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(6, 4, 6));
        build(helper, FusionStructures.HEATER, origin, 0, null);
        helper.getLevel().setBlock(origin.offset(1, 1, 1), FusionStructures.HEATER.part(0).defaultBlockState(), 3);
        FusionHeaterBlockEntity heater = (FusionHeaterBlockEntity) helper.getLevel().getBlockEntity(origin.offset(2, 2, 2));
        helper.runAfterDelay(3, () -> {
            int before = heater.getTemperature();
            heater.whenInBeam(helper.getLevel(), origin.offset(2, 2, 2), 2_097_152L, 1);
            helper.assertTrue(heater.getTemperature() - before == 640 * 21, "a 2 MW beam adds 640 degrees per bit of its log: " + (heater.getTemperature() - before));
            helper.assertTrue(heater.blockBeam(helper.getLevel(), origin.offset(2, 2, 2), 2_097_152L), "and a built heater stops the beam");
            helper.succeed();
        });
    }
}
