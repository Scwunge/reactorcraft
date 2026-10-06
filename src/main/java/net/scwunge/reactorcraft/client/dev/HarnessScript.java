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
import net.scwunge.reactorcraft.content.material.FluoriteColor;
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
            check("creative tab lists every item", ReactorTabs.MAIN.get().getDisplayItems().size() == ReactorItems.TAB.size());
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
