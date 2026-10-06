package net.scwunge.reactorcraft.client.dev;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.block.FluoriteBlock;
import net.scwunge.reactorcraft.content.machine.ElectrolyzerBlockEntity;
import net.scwunge.reactorcraft.content.machine.FluidSynthesizerBlockEntity;
import net.scwunge.reactorcraft.content.machine.IsotopeCentrifugeBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMenu;
import net.scwunge.reactorcraft.content.machine.UraniumProcessorBlockEntity;
import net.scwunge.reactorcraft.content.machine.WasteStorageBlockEntity;
import net.scwunge.reactorcraft.content.material.FluoriteColor;
import net.scwunge.reactorcraft.content.waste.Isotope;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorItems;
import net.scwunge.reactorcraft.registry.ReactorTabs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static net.scwunge.reactorcraft.client.dev.DevHarness.add;
import static net.scwunge.reactorcraft.client.dev.DevHarness.check;
import static net.scwunge.reactorcraft.client.dev.DevHarness.level;
import static net.scwunge.reactorcraft.client.dev.DevHarness.player;
import static net.scwunge.reactorcraft.client.dev.DevHarness.server;
import static net.scwunge.reactorcraft.client.dev.DevHarness.shot;

/** The scripted in-game checks run by {@link DevHarness}; -PharnessOnly=<section> runs one section. */
final class HarnessScript {
    /** Flat world surface: blocks stand on y = -60. */
    static final int Y = -60;

    private HarnessScript() {
    }

    static void build() {
        String only = System.getProperty("reactorcraft.harness.only", "");
        if (only.isEmpty() || only.equals("materials")) {
            materials();
            creativeTab();
        }
        if (only.isEmpty() || only.equals("machines")) {
            machines();
        }
        if (only.isEmpty() || only.equals("core")) {
            core();
        }
    }

    static void look(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        player.teleportTo(player.serverLevel(), x, y, z, yaw, pitch);
    }

    /** Every ore and material block in a row, with the fluorite blocks lit on the second row. */
    private static void materials() {
        List<Block> row = new ArrayList<>(List.of(ReactorBlocks.PITCHBLENDE_ORE.get(), ReactorBlocks.CADMIUM_ORE.get(), ReactorBlocks.INDIUM_ORE.get(),
                ReactorBlocks.SILVER_ORE.get(), ReactorBlocks.ENDBLENDE_ORE.get(), ReactorBlocks.AMMONIUM_CHLORIDE_ORE.get(),
                ReactorBlocks.CALCITE_ORE.get(), ReactorBlocks.MAGNETITE_ORE.get(), ReactorBlocks.THORITE_ORE.get(),
                ReactorBlocks.CONCRETE.get(), ReactorBlocks.CORIUM_BLOCK.get(), ReactorBlocks.CALCITE_BLOCK.get(),
                ReactorBlocks.STEAM_SCRUBBER.get(), ReactorBlocks.LODESTONE_BLOCK.get(), ReactorBlocks.GRAPHITE_BLOCK.get()));
        server(40, server -> {
            ServerLevel level = level(server);
            ServerPlayer player = player(server);
            player.setGameMode(GameType.CREATIVE);
            for (int i = 0; i < row.size(); i++) {
                level.setBlockAndUpdate(new BlockPos(i, Y + 1, 6), row.get(i).defaultBlockState());
            }
            FluoriteColor[] colors = FluoriteColor.values();
            for (int i = 0; i < colors.length; i++) {
                level.setBlockAndUpdate(new BlockPos(i, Y + 1, 8), ReactorBlocks.fluoriteOre(colors[i]).defaultBlockState());
                BlockState lit = ReactorBlocks.fluoriteBlock(colors[i]).defaultBlockState().setValue(FluoriteBlock.ACTIVE, i % 2 == 0);
                level.setBlockAndUpdate(new BlockPos(i, Y + 2, 8), lit);
            }
            level.setDayTime(18000);
            look(player, 7.5, Y + 3.5, 1.5, 0, 25);
        });
        shot("materials");
        server(5, server -> level(server).setDayTime(6000));
    }

    private static void fill(net.neoforged.neoforge.fluids.capability.templates.FluidTank tank, net.minecraft.world.level.material.Fluid fluid, int amount) {
        tank.setFluid(new FluidStack(fluid, amount));
    }

    /** One of each machine, some with fluid in them, then each GUI opened in turn. */
    private static void machines() {
        BlockPos[] spots = new BlockPos[9];
        for (int i = 0; i < spots.length; i++) {
            spots[i] = new BlockPos(i * 2, Y + 1, 6);
        }
        server(40, server -> {
            ServerLevel level = level(server);
            ServerPlayer player = player(server);
            player.setGameMode(GameType.CREATIVE);
            level.setBlockAndUpdate(spots[0], ReactorBlocks.FLUID_EXTRACTOR.get().defaultBlockState());
            level.setBlockAndUpdate(spots[1], ReactorBlocks.ISOTOPE_CENTRIFUGE.get().defaultBlockState());
            level.setBlockAndUpdate(spots[2], ReactorBlocks.URANIUM_PROCESSOR.get().defaultBlockState());
            level.setBlockAndUpdate(spots[3], ReactorBlocks.ELECTROLYZER.get().defaultBlockState());
            level.setBlockAndUpdate(spots[4], ReactorBlocks.FLUID_SYNTHESIZER.get().defaultBlockState());
            level.setBlockAndUpdate(spots[5], ReactorBlocks.WASTE_CONTAINER.get().defaultBlockState());
            level.setBlockAndUpdate(spots[6], ReactorBlocks.WASTE_STORAGE.get().defaultBlockState());
            level.setBlockAndUpdate(spots[7], ReactorBlocks.GAS_COLLECTOR.get().defaultBlockState());
            for (int h = 0; h < 3; h++) {
                level.setBlockAndUpdate(spots[8].above(h), ReactorBlocks.WASTE_DECAYER.get().defaultBlockState());
            }
            level.setBlockAndUpdate(spots[7].south(), net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());
            UraniumProcessorBlockEntity processor = (UraniumProcessorBlockEntity) level.getBlockEntity(spots[2]);
            fill(processor.inputTank(), Fluids.WATER, 2000);
            fill(processor.intermediateTank(), ReactorFluids.HYDROFLUORIC_ACID.get(), 1200);
            fill(processor.outputTank(), ReactorFluids.URANIUM_HEXAFLUORIDE.get(), 2500);
            ElectrolyzerBlockEntity electrolyzer = (ElectrolyzerBlockEntity) level.getBlockEntity(spots[3]);
            fill(electrolyzer.inputTank(), ReactorFluids.HEAVY_WATER.get(), 7000);
            fill(electrolyzer.lightTank(), ReactorFluids.DEUTERIUM.get(), 3000);
            fill(electrolyzer.heavyTank(), ReactorFluids.OXYGEN.get(), 1500);
            FluidSynthesizerBlockEntity synthesizer = (FluidSynthesizerBlockEntity) level.getBlockEntity(spots[4]);
            fill(synthesizer.waterTank(), Fluids.WATER, 15000);
            fill(synthesizer.productTank(), ReactorFluids.AMMONIA.get(), 6000);
            IsotopeCentrifugeBlockEntity centrifuge = (IsotopeCentrifugeBlockEntity) level.getBlockEntity(spots[1]);
            fill(centrifuge.tank(), ReactorFluids.URANIUM_HEXAFLUORIDE.get(), 8000);
            WasteStorageBlockEntity storage = (WasteStorageBlockEntity) level.getBlockEntity(spots[6]);
            storage.items().setStackInSlot(0, WasteManager.waste(Isotope.CS137, 5));
            storage.items().setStackInSlot(3, WasteManager.waste(Isotope.U238, 12));
            storage.items().setStackInSlot(7, WasteManager.mixedWaste(Isotope.ElementGroup.ALKALI));
            level.setDayTime(6000);
            look(player, 0.5, Y + 2.2, 2.6, 0, 12);
        });
        String[] model = {"fluid-extractor", "isotope-centrifuge", "uranium-processor", "electrolyzer", "fluid-synthesizer", "waste-container",
                "waste-storage", "gas-collector", "waste-decayer"};
        for (int i = 0; i < spots.length; i++) {
            BlockPos at = spots[i];
            server(i == 0 ? 15 : 10, server -> look(player(server), at.getX() + 0.5, Y + (at.getX() == 16 ? 3.2 : 2.2), 2.6, 0, at.getX() == 16 ? 0 : 12));
            shot("machine-" + model[i]);
        }
        // the dropped items: nuclear waste and the machine items in the hand-held pose
        String[] names = {"fluid-extractor", "isotope-centrifuge", "uranium-processor", "electrolyzer", "fluid-synthesizer", "waste-container",
                "waste-storage", "gas-collector", "waste-decayer"};
        for (int i = 0; i < names.length; i++) {
            if (i == 0 || i == 7) {
                continue; // these have no GUI
            }
            BlockPos at = spots[i];
            server(5, server -> {
                if (level(server).getBlockEntity(at) instanceof ReactorMachineBlockEntity machine) {
                    ReactorMenu.open(player(server), machine);
                }
            });
            add((mc, server) -> 10);
            shot("gui-" + names[i]);
            add((mc, server) -> {
                mc.player.closeContainer();
                return 5;
            });
        }
    }

    /** A small fission core: a fuel column, coolant cells in every state, control rods up and down, with neutrons flying. */
    private static void core() {
        server(40, server -> {
            ServerLevel level = level(server);
            ServerPlayer player = player(server);
            player.setGameMode(GameType.CREATIVE);
            for (int dx = -1; dx <= 9; dx++) {
                for (int dz = 3; dz <= 9; dz++) {
                    for (int dy = 1; dy <= 4; dy++) {
                        level.setBlockAndUpdate(new BlockPos(dx, Y + dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            }
            for (int h = 0; h < 3; h++) {
                level.setBlockAndUpdate(new BlockPos(2, Y + 1 + h, 6), ReactorBlocks.FUEL_ROD.get().defaultBlockState());
            }
            net.scwunge.reactorcraft.content.machine.FuelRodBlockEntity top =
                    (net.scwunge.reactorcraft.content.machine.FuelRodBlockEntity) level.getBlockEntity(new BlockPos(2, Y + 3, 6));
            top.items().setStackInSlot(3, new ItemStack(ReactorItems.FUEL.get()));
            top.items().setStackInSlot(4, WasteManager.waste(Isotope.CS137));
            net.scwunge.reactorcraft.core.CoolantState[] states = net.scwunge.reactorcraft.core.CoolantState.values();
            for (int i = 0; i < states.length; i++) {
                BlockPos at = new BlockPos(4 + i - 1 + (i == 0 ? 0 : 0), Y + 1, 4);
                level.setBlockAndUpdate(at, ReactorBlocks.COOLANT_CELL.get().defaultBlockState());
                ((net.scwunge.reactorcraft.content.machine.CoolantCellBlockEntity) level.getBlockEntity(at)).setCoolant(states[i]);
            }
            level.setBlockAndUpdate(new BlockPos(0, Y + 1, 6), ReactorBlocks.CONTROL_ROD.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(6, Y + 1, 6), ReactorBlocks.CONTROL_ROD.get().defaultBlockState());
            ((net.scwunge.reactorcraft.content.machine.ControlRodBlockEntity) level.getBlockEntity(new BlockPos(6, Y + 1, 6))).setActive(false, false);
            level.setDayTime(6000);
            look(player, 3.5, Y + 2.6, 1.5, 0, 15);
        });
        server(30, server -> {
            ServerLevel level = level(server);
            for (int i = 0; i < 4; i++) {
                level.addFreshEntity(new net.scwunge.reactorcraft.content.entity.NeutronEntity(level, new BlockPos(1 + i, Y + 2, 5),
                        net.minecraft.core.Direction.EAST, i % 2 == 0 ? net.scwunge.reactorcraft.core.NeutronType.FISSION : net.scwunge.reactorcraft.core.NeutronType.DECAY));
            }
        });
        shot("core-1");
        server(5, server -> {
            if (level(server).getBlockEntity(new BlockPos(2, Y + 3, 6)) instanceof ReactorMachineBlockEntity machine) {
                ReactorMenu.open(player(server), machine);
            }
        });
        add((mc, server) -> 10);
        shot("gui-fuel-rod");
        add((mc, server) -> {
            mc.player.closeContainer();
            return 5;
        });
    }

    /** All mod items in the creative tab, page by page. */
    private static void creativeTab() {
        add((mc, server) -> {
            CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
            mc.setScreen(screen);
            try {
                Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                select.setAccessible(true);
                select.invoke(screen, ReactorTabs.MAIN.get());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            return 10;
        });
        add((mc, server) -> {
            check("creative tab lists every item", ReactorTabs.MAIN.get().getDisplayItems().size() == ReactorItems.TAB.size() + WasteManager.creativeStacks().size());
            return 1;
        });
        int rows = (ReactorItems.TAB.size() + 8) / 9;
        int pages = Math.max(1, (rows - 5 + 4) / 5 + 1);
        for (int page = 0; page < pages; page++) {
            float scroll = pages == 1 ? 0 : page / (float) (pages - 1);
            add((mc, server) -> {
                if (mc.screen instanceof CreativeModeInventoryScreen screen) {
                    screen.getMenu().scrollTo(scroll);
                }
                return 5;
            });
            shot("creative-tab-" + (page + 1));
        }
        add((mc, server) -> {
            mc.player.closeContainer();
            return 5;
        });
    }
}
