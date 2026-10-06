package net.scwunge.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.WasteContainerBlockEntity;
import net.scwunge.reactorcraft.content.machine.WasteDecayerBlockEntity;
import net.scwunge.reactorcraft.content.machine.WasteStorageBlockEntity;
import net.scwunge.reactorcraft.content.waste.Isotope;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

import java.lang.reflect.Field;

/** Milestone 3: nuclear waste and the units that hold it. */
@GameTestHolder(ReactorCraft.MODID)
@PrefixGameTestTemplate(false)
public final class WasteTests {
    private static final String EMPTY = MaterialTests.EMPTY;
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private WasteTests() {
    }

    private static int count(net.neoforged.neoforge.items.IItemHandlerModifiable items, Isotope isotope) {
        int n = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            if (WasteManager.isotope(items.getStackInSlot(i)) == isotope) {
                n += items.getStackInSlot(i).getCount();
            }
        }
        return n;
    }

    private static void setTemperature(ReactorBlockEntity be, int temperature) throws ReflectiveOperationException {
        Field field = ReactorBlockEntity.class.getDeclaredField("temperature");
        field.setAccessible(true);
        field.setInt(be, temperature);
    }

    @GameTest(template = EMPTY)
    public static void isotopeTableMatchesTheOriginal(GameTestHelper helper) {
        helper.assertTrue(Isotope.count() == 33, "isotope count " + Isotope.count());
        helper.assertTrue(Isotope.U235.halfLifeText().equals("704.000My"), "U235 half-life shows as " + Isotope.U235.halfLifeText());
        helper.assertTrue(Isotope.CS137.halfLifeText().equals("30.170y"), "Cs137 half-life shows as " + Isotope.CS137.halfLifeText());
        helper.assertTrue(Isotope.CS137.isLongLived() && Isotope.U238.isLongLived(), "Cs137 and U238 outlive six years");
        helper.assertTrue(!Isotope.CS134.isLongLived() && !Isotope.XE135.isLongLived() && !Isotope.I131.isLongLived(), "short-lived ones");
        helper.assertTrue(Isotope.PU239.extraDanger && !Isotope.U238.extraDanger, "danger flags");
        helper.assertTrue(Isotope.U238.decay().product().isotope() == Isotope.PU239 && Isotope.U238.decay().amount() == 1,
                "U238 should decay into one Pu239: " + Isotope.U238.decay());
        helper.assertTrue(Isotope.PU239.decay().product().isotope() == Isotope.U235, "Pu239 should decay into U235");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void wasteItemsCarryTheirIsotope(GameTestHelper helper) {
        ItemStack cs = WasteManager.waste(Isotope.CS137, 3);
        helper.assertTrue(WasteManager.isotope(cs) == Isotope.CS137 && cs.getCount() == 3, "isotope round trip");
        ItemStack mixed = WasteManager.mixedWaste(Isotope.ElementGroup.TRANSITION);
        helper.assertTrue(WasteManager.isMixed(mixed) && WasteManager.group(mixed) == Isotope.ElementGroup.TRANSITION
                && WasteManager.isotope(mixed) == null, "mixed waste round trip");
        helper.assertTrue(WasteManager.isotope(new ItemStack(Items.STICK)) == null, "a stick is not waste");
        helper.assertTrue(WasteManager.creativeStacks().size() == 18 + 4, "creative list: " + WasteManager.creativeStacks().size());
        for (int i = 0; i < 200; i++) {
            Isotope rolled = WasteManager.randomWaste(helper.getLevel().random);
            helper.assertTrue(WasteManager.uraniumYields().containsKey(rolled), "rolled " + rolled);
            helper.assertTrue(WasteManager.thoriumYields().containsKey(WasteManager.randomThoriumWaste(helper.getLevel().random)), "thorium roll");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void containerTakesShortLivedAndStorageLongLived(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.WASTE_CONTAINER.get());
        helper.setBlock(AT.east(), ReactorBlocks.WASTE_STORAGE.get());
        WasteContainerBlockEntity container = helper.getBlockEntity(AT);
        WasteStorageBlockEntity storage = helper.getBlockEntity(AT.east());
        ItemStack short1 = WasteManager.waste(Isotope.XE135);
        ItemStack long1 = WasteManager.waste(Isotope.CS137);
        helper.assertTrue(container.isItemValid(0, short1) && !container.isItemValid(0, long1), "container rules");
        helper.assertTrue(!storage.isItemValid(0, short1) && storage.isItemValid(0, long1), "storage rules");
        helper.assertTrue(!container.isItemValid(0, new ItemStack(Items.STICK)), "container took a stick");
        helper.assertTrue(!container.isItemValid(0, WasteManager.mixedWaste(Isotope.ElementGroup.ALKALI)), "container took mixed waste");
        helper.succeed();
    }

    private static ItemStack insertAnywhere(IItemHandler handler, ItemStack stack) {
        for (int i = 0; i < handler.getSlots() && !stack.isEmpty(); i++) {
            stack = handler.insertItem(i, stack, false);
        }
        return stack;
    }

    private static int usedSlots(net.neoforged.neoforge.items.IItemHandlerModifiable items) {
        int slots = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                slots++;
            }
        }
        return slots;
    }

    @GameTest(template = EMPTY)
    public static void storageStacksSameWasteTogether(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.WASTE_STORAGE.get());
        WasteStorageBlockEntity storage = helper.getBlockEntity(AT);
        IItemHandler handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(AT), Direction.UP);
        helper.assertTrue(handler != null, "no item handler");
        helper.assertTrue(insertAnywhere(handler, WasteManager.waste(Isotope.U238, 10)).isEmpty(), "10 waste did not fit");
        // like the original, a stack only joins another if all of it fits (16 to a slot)
        helper.assertTrue(insertAnywhere(handler, WasteManager.waste(Isotope.U238, 6)).isEmpty(), "6 more waste did not fit");
        helper.assertTrue(usedSlots(storage.items()) == 1 && count(storage.items(), Isotope.U238) == 16,
                "16 of the same waste should share a slot, used " + usedSlots(storage.items()));
        helper.assertTrue(insertAnywhere(handler, WasteManager.waste(Isotope.U238, 1)).isEmpty() && usedSlots(storage.items()) == 2,
                "a seventeenth goes in a new slot");
        helper.assertTrue(handler.extractItem(0, 1, true).isEmpty(), "automation took waste out of storage");
        helper.assertTrue(storage.range() == 4 && storage.maxRange() == 3, "range " + storage.range() + ", max " + storage.maxRange());
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void shortLivedWasteDecaysInAContainer(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.WASTE_CONTAINER.get());
        WasteContainerBlockEntity be = helper.getBlockEntity(AT);
        be.items().setStackInSlot(0, WasteManager.waste(Isotope.RU103));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(be.countWaste() == 0, "Ru103 (half-life under 2 ticks) should be gone, found " + be.countWaste());
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void stableWasteStaysPut(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.WASTE_STORAGE.get());
        WasteStorageBlockEntity be = helper.getBlockEntity(AT);
        be.items().setStackInSlot(0, WasteManager.waste(Isotope.XE136, 5));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(be.countWaste() == 5, "Xe136 is basically stable but " + be.countWaste() + " of 5 are left");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void containersPassWasteDownAColumn(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.WASTE_CONTAINER.get());
        helper.setBlock(AT.above(), ReactorBlocks.WASTE_CONTAINER.get());
        WasteContainerBlockEntity bottom = helper.getBlockEntity(AT);
        WasteContainerBlockEntity top = helper.getBlockEntity(AT.above());
        top.items().setStackInSlot(0, WasteManager.waste(Isotope.CS134));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(count(top.items(), Isotope.CS134) == 0, "the upper container kept its waste");
            helper.assertTrue(count(bottom.items(), Isotope.CS134) == 1, "the lower container has " + count(bottom.items(), Isotope.CS134));
            helper.assertTrue(!bottom.items().getStackInSlot(bottom.items().getSlots() - 1).isEmpty(), "waste should settle in the last slot");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void decayerTurnsLongLivedWasteIntoItsDaughter(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.WASTE_DECAYER.get());
        WasteDecayerBlockEntity be = helper.getBlockEntity(AT);
        be.items().setStackInSlot(0, WasteManager.waste(Isotope.U238));
        helper.runAfterDelay(3, () -> {
            try {
                setTemperature(be, WasteDecayerBlockEntity.OPTIMAL_TEMPERATURE);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            helper.assertTrue(be.decayChance() == 25, "chance at 400 C is " + be.decayChance());
            int absorbed = 0;
            for (int i = 0; i < 500; i++) {
                if (be.onNeutron(NeutronType.FISSION, NeutronType.NeutronSpeed.THERMAL)) {
                    absorbed++;
                }
            }
            helper.assertTrue(absorbed > 150 && absorbed < 350, "about half of 500 neutrons should be absorbed: " + absorbed);
            helper.assertTrue(count(be.items(), Isotope.U238) == 0, "U238 survived 500 neutrons");
            helper.assertTrue(be.countWaste() >= 1, "nothing was made");
            helper.assertTrue(!be.onNeutron(NeutronType.WASTE, NeutronType.NeutronSpeed.THERMAL), "waste neutrons cannot irradiate");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void decayerRoutesWasteOneWay(GameTestHelper helper) {
        helper.setBlock(AT, ReactorBlocks.WASTE_DECAYER.get());
        WasteDecayerBlockEntity be = helper.getBlockEntity(AT);
        ItemStack waste = WasteManager.waste(Isotope.CS137);
        helper.assertTrue(be.canInsertFromSide(0, waste, Direction.UP) && !be.canInsertFromSide(0, waste, Direction.NORTH), "enters from the top only");
        helper.assertTrue(!be.canExtractFromSide(0, Direction.DOWN) || be.items().getStackInSlot(0).isEmpty(), "empty slot extraction check");
        be.items().setStackInSlot(0, WasteManager.waste(Isotope.CS137));
        be.items().setStackInSlot(1, WasteManager.waste(Isotope.XE135));
        helper.assertTrue(!be.canExtractFromSide(0, Direction.DOWN), "long-lived waste must stay in");
        helper.assertTrue(be.canExtractFromSide(1, Direction.DOWN) && !be.canExtractFromSide(1, Direction.UP), "short-lived waste leaves from the bottom");
        helper.succeed();
    }
}
