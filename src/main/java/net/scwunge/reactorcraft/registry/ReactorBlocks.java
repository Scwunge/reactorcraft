package net.scwunge.reactorcraft.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.block.FluoriteBlock;
import net.scwunge.reactorcraft.content.block.FluoriteOreBlock;
import net.scwunge.reactorcraft.content.block.LodestoneBlock;
import net.scwunge.reactorcraft.content.material.FluoriteColor;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** ReactorCraft's blocks; each gets a block item in the creative tab. */
public final class ReactorBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ReactorCraft.MODID);

    // ores (ReactorOres; hardness 2, resistance 5; they drop themselves and give their xp when smelted)
    public static final DeferredBlock<Block> PITCHBLENDE_ORE = ore("pitchblende_ore", MapColor.STONE);
    public static final DeferredBlock<Block> CADMIUM_ORE = ore("cadmium_ore", MapColor.STONE);
    public static final DeferredBlock<Block> INDIUM_ORE = ore("indium_ore", MapColor.STONE);
    public static final DeferredBlock<Block> SILVER_ORE = ore("silver_ore", MapColor.STONE);
    public static final DeferredBlock<Block> ENDBLENDE_ORE = ore("endblende_ore", MapColor.SAND);
    public static final DeferredBlock<Block> AMMONIUM_CHLORIDE_ORE = ore("ammonium_chloride_ore", MapColor.NETHER);
    public static final DeferredBlock<Block> CALCITE_ORE = ore("calcite_ore", MapColor.STONE);
    public static final DeferredBlock<Block> MAGNETITE_ORE = ore("magnetite_ore", MapColor.STONE);
    public static final DeferredBlock<Block> THORITE_ORE = ore("thorite_ore", MapColor.NETHER);

    private static final Map<FluoriteColor, DeferredBlock<FluoriteOreBlock>> FLUORITE_ORE = new EnumMap<>(FluoriteColor.class);
    private static final Map<FluoriteColor, DeferredBlock<FluoriteBlock>> FLUORITE_BLOCK = new EnumMap<>(FluoriteColor.class);

    static {
        for (FluoriteColor color : FluoriteColor.values()) {
            FLUORITE_ORE.put(color, block(color.getSerializedName() + "_fluorite_ore", () -> new FluoriteOreBlock(color,
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2F, 5F).requiresCorrectToolForDrops())));
        }
        for (FluoriteColor color : FluoriteColor.values()) {
            FLUORITE_BLOCK.put(color, block(color.getSerializedName() + "_fluorite_block", () -> new FluoriteBlock(color,
                    BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(1.2F, 4F).sound(SoundType.AMETHYST)
                            .requiresCorrectToolForDrops())));
        }
    }

    // material blocks (MatBlocks; hardness 1.5, resistance 10)
    public static final DeferredBlock<Block> CONCRETE = block("concrete", () -> new Block(mat(MapColor.STONE)));
    public static final DeferredBlock<Block> CORIUM_BLOCK = block("corium_block", () -> new Block(mat(MapColor.COLOR_BLACK).randomTicks()));
    public static final DeferredBlock<Block> CALCITE_BLOCK = block("calcite_block", () -> new Block(mat(MapColor.QUARTZ)));
    public static final DeferredBlock<Block> STEAM_SCRUBBER = block("steam_scrubber", () -> new Block(mat(MapColor.METAL)
            .noCollission().noOcclusion().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> LODESTONE_BLOCK = block("lodestone_block", () -> new LodestoneBlock(mat(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<Block> GRAPHITE_BLOCK = block("graphite_block", () -> new Block(mat(MapColor.COLOR_BLACK)));

    private ReactorBlocks() {
    }

    public static FluoriteOreBlock fluoriteOre(FluoriteColor color) {
        return FLUORITE_ORE.get(color).get();
    }

    public static FluoriteBlock fluoriteBlock(FluoriteColor color) {
        return FLUORITE_BLOCK.get(color).get();
    }

    private static BlockBehaviour.Properties mat(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(1.5F, 10F).requiresCorrectToolForDrops();
    }

    private static DeferredBlock<Block> ore(String name, MapColor color) {
        return block(name, () -> new Block(BlockBehaviour.Properties.of().mapColor(color).strength(2F, 5F).requiresCorrectToolForDrops()));
    }

    static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> factory) {
        DeferredBlock<T> block = BLOCKS.register(name, factory);
        ReactorItems.add(ReactorItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
        return block;
    }
}
