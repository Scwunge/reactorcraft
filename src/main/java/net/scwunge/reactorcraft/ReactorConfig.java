package net.scwunge.reactorcraft;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** Server settings: the original's ReactorOptions that affect gameplay, plus switches for server safety. */
public final class ReactorConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FAST_NEUTRONS;
    public static final ModConfigSpec.BooleanValue VERTICAL_NEUTRONS;
    public static final ModConfigSpec.BooleanValue CHUNKLOADING;
    public static final ModConfigSpec.IntValue TOROID_CHARGE_DELAY;
    public static final ModConfigSpec.IntValue STEAM_LINE_CAPACITY;
    public static final ModConfigSpec.BooleanValue RADIOACTIVE_ORES;
    public static final ModConfigSpec.DoubleValue LODESTONE_FE_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HEAVY_WATER_DIMENSIONS;

    public static final ModConfigSpec.BooleanValue MELTDOWNS_DESTROY_BLOCKS;
    public static final ModConfigSpec.BooleanValue RADIATION_TRANSFORMS_BLOCKS;
    public static final ModConfigSpec.BooleanValue HOT_BLOCKS_AFFECT_WORLD;

    static {
        BUILDER.push("reactors");
        FAST_NEUTRONS = BUILDER.comment("Neutrons are fast or thermal and need moderating (heavy water) to cause fission well, as in real reactors.")
                .define("fastNeutrons", false);
        VERTICAL_NEUTRONS = BUILDER.comment("Fission neutrons can also travel up and down, not just sideways.")
                .define("verticalNeutrons", false);
        CHUNKLOADING = BUILDER.comment("Active fission cores keep their chunk and the ones around it loaded.")
                .define("chunkloading", true);
        TOROID_CHARGE_DELAY = BUILDER.comment("How many ticks toroid magnets take to pass charge along; lower costs more server time.")
                .defineInRange("toroidChargeDelay", 4, 1, 40);
        STEAM_LINE_CAPACITY = BUILDER.comment("Most steam one steam line block can hold.")
                .defineInRange("steamLineCapacity", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        RADIOACTIVE_ORES = BUILDER.comment("Pitchblende and thorium ores are weakly radioactive.")
                .define("radioactiveOres", false);
        LODESTONE_FE_MULTIPLIER = BUILDER.comment("Energy a powered lodestone block feeds into the block on top of it, as a multiplier.")
                .defineInRange("lodestoneEnergyMultiplier", 1.0, 0.0, 1000.0);
        HEAVY_WATER_DIMENSIONS = BUILDER.comment("Dimensions where the Centrifugal Fluid Extractor finds heavy water in the sea (empty for all).")
                .defineListAllowEmpty("heavyWaterDimensions", List.of(), () -> "minecraft:overworld", o -> o instanceof String s && ResourceLocation.tryParse(s) != null);
        BUILDER.pop();

        BUILDER.push("safety");
        MELTDOWNS_DESTROY_BLOCKS = BUILDER.comment("Meltdowns and hydrogen explosions break blocks and pour corium (also needs the mobGriefing game rule;",
                        "claimed land is left alone). False keeps the damage to the reactor itself and nearby creatures.")
                .define("meltdownsDestroyBlocks", true);
        RADIATION_TRANSFORMS_BLOCKS = BUILDER.comment("Radiation and stray neutrons kill plants and change blocks (also needs mobGriefing; claims apply).")
                .define("radiationTransformsBlocks", true);
        HOT_BLOCKS_AFFECT_WORLD = BUILDER.comment("Very hot reactor parts melt snow and ice, boil water and set fire to things next to them.")
                .define("hotBlocksAffectWorld", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ReactorConfig() {
    }
}
