package net.scwunge.reactorcraft.client.dev;

import net.scwunge.reactorcraft.ReactorCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Development test harness, off unless the client runs with {@code -Dreactorcraft.harness=true}. It creates a fresh flat
 * creative world, runs the scripted steps in {@link HarnessScript} (set up machines on the server, wait, open GUIs,
 * take screenshots into {@code runs/client/screenshots}), logs each check, and quits. Lets every feature be looked at
 * in a real client without anyone at the keyboard.
 */
@EventBusSubscriber(modid = ReactorCraft.MODID, value = Dist.CLIENT)
public final class DevHarness {
    public static final boolean ENABLED = Boolean.getBoolean("reactorcraft.harness");
    static final String WORLD = "reactorcraft-harness";

    private static final Deque<Step> steps = new ArrayDeque<>();
    private static boolean hidden;
    private static boolean worldRequested;
    private static boolean scripted;
    private static int wait;
    private static int failures;

    private DevHarness() {
    }

    /** One scripted action; returns how many ticks to wait before the next one. */
    @FunctionalInterface
    public interface Step {
        int run(Minecraft mc, MinecraftServer server);
    }

    static void add(Step step) {
        steps.add(step);
    }

    /** Runs {@code action} on the server thread, then waits {@code ticks}. */
    static void server(int ticks, Consumer<MinecraftServer> action) {
        add((mc, server) -> {
            server.execute(() -> action.accept(server));
            return ticks;
        });
    }

    static void shot(String name) {
        add((mc, server) -> {
            mc.gui.getChat().clearMessages(false);
            Screenshot.grab(mc.gameDirectory, "reactorcraft-" + name + ".png", mc.getMainRenderTarget(), msg -> {
            });
            ReactorCraft.LOGGER.info("[harness] screenshot {}", name);
            return 5;
        });
    }

    static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        ReactorCraft.LOGGER.info("[harness] {} {}", ok ? "PASS" : "FAIL", what);
    }

    static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    static ServerLevel level(MinecraftServer server) {
        return server.overworld();
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!hidden) {
            // other people use this PC: get the window out of the way; the game keeps rendering for screenshots
            hidden = true;
            GLFW.glfwHideWindow(mc.getWindow().getWindow());
        }
        if (scripted && mc.level == null) {
            // kicked out of the world (a crash in a screen, for example): don't sit there forever
            ReactorCraft.LOGGER.error("[harness] FAIL disconnected from the world, {} failure(s) before that", failures);
            scripted = false;
            mc.stop();
            return;
        }
        if (mc.level == null) {
            if (!worldRequested && mc.screen instanceof TitleScreen) {
                worldRequested = true;
                lightSettings(mc);
                createWorld(mc);
            }
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null || server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        if (!scripted) {
            scripted = true;
            wait = 100; // let the world settle and chunks render
            server.execute(() -> {
                ServerLevel level = level(server);
                level.setDayTime(6000);
                level.setWeatherParameters(1_000_000, 0, false, false);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            });
            HarnessScript.build();
            add((m, s) -> {
                ReactorCraft.LOGGER.info("[harness] done, {} failure(s)", failures);
                m.stop();
                return Integer.MAX_VALUE;
            });
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = steps.poll();
        if (step != null) {
            try {
                wait = step.run(mc, server);
            } catch (RuntimeException e) {
                failures++;
                ReactorCraft.LOGGER.error("[harness] FAIL step threw", e);
                wait = 5;
            }
        }
    }

    /** Keeps the test client light: the PC is shared. */
    private static void lightSettings(Minecraft mc) {
        mc.options.renderDistance().set(4);
        mc.options.simulationDistance().set(5);
        mc.options.framerateLimit().set(30);
        mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.onboardAccessibility = false;
        mc.options.save();
    }

    private static void createWorld(Minecraft mc) {
        LevelSettings settings = new LevelSettings(WORLD + "-" + System.currentTimeMillis(), GameType.CREATIVE, false, Difficulty.PEACEFUL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(1L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                mc.screen);
    }
}
