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
import net.scwunge.reactorcraft.content.block.SteamBlock;
import net.scwunge.reactorcraft.content.machine.BoilerBlock;
import net.scwunge.reactorcraft.content.machine.ControlRodBlock;
import net.scwunge.reactorcraft.content.machine.SteamGrateBlock;
import net.scwunge.reactorcraft.content.machine.TurbineCoreBlock;
import net.scwunge.reactorcraft.content.machine.TurbineMeterBlock;
import net.scwunge.reactorcraft.content.machine.SteamLineBlock;
import net.scwunge.reactorcraft.content.machine.CpuBlock;
import net.scwunge.reactorcraft.content.machine.CoolantCellBlock;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;
import net.scwunge.reactorcraft.content.machine.StackableMachineBlock;
import net.scwunge.reactorcraft.content.machine.WasteDecayerBlock;
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

    // machines (BlockReactorTile; hardness 2, resistance 10, iron)
    public static final DeferredBlock<ReactorMachineBlock> FLUID_EXTRACTOR = block("fluid_extractor",
            () -> new ReactorMachineBlock(machine().noOcclusion(), ReactorBlockEntities.FLUID_EXTRACTOR, true, true));
    public static final DeferredBlock<ReactorMachineBlock> ISOTOPE_CENTRIFUGE = block("isotope_centrifuge",
            () -> new ReactorMachineBlock(machine().noOcclusion(), ReactorBlockEntities.ISOTOPE_CENTRIFUGE, true, true));
    public static final DeferredBlock<ReactorMachineBlock> URANIUM_PROCESSOR = block("uranium_processor",
            () -> new ReactorMachineBlock(machine().noOcclusion(), ReactorBlockEntities.URANIUM_PROCESSOR, true, false));
    public static final DeferredBlock<ReactorMachineBlock> ELECTROLYZER = block("electrolyzer",
            () -> new ReactorMachineBlock(machine().noOcclusion(), ReactorBlockEntities.ELECTROLYZER, true, false));
    public static final DeferredBlock<ReactorMachineBlock> FLUID_SYNTHESIZER = block("fluid_synthesizer",
            () -> new ReactorMachineBlock(machine(), ReactorBlockEntities.FLUID_SYNTHESIZER, false, false));
    public static final DeferredBlock<ReactorMachineBlock> GAS_COLLECTOR = block("gas_collector",
            () -> new ReactorMachineBlock(machine().noOcclusion(), ReactorBlockEntities.GAS_COLLECTOR, true, true, true));

    public static final DeferredBlock<ReactorMachineBlock> WASTE_CONTAINER = block("waste_container",
            () -> new ReactorMachineBlock(machine(), ReactorBlockEntities.WASTE_CONTAINER, false, false));
    public static final DeferredBlock<ReactorMachineBlock> WASTE_STORAGE = block("waste_storage",
            () -> new ReactorMachineBlock(machine().noOcclusion(), ReactorBlockEntities.WASTE_STORAGE, true, false));
    public static final DeferredBlock<WasteDecayerBlock> WASTE_DECAYER = block("waste_decayer",
            () -> new WasteDecayerBlock(machine(), ReactorBlockEntities.WASTE_DECAYER));

    public static final DeferredBlock<StackableMachineBlock> FUEL_ROD = block("fuel_rod",
            () -> new StackableMachineBlock(machine(), ReactorBlockEntities.FUEL_ROD));
    public static final DeferredBlock<ControlRodBlock> CONTROL_ROD = block("control_rod",
            () -> new ControlRodBlock(machine().noOcclusion(), ReactorBlockEntities.CONTROL_ROD));
    public static final DeferredBlock<CoolantCellBlock> COOLANT_CELL = block("coolant_cell",
            () -> new CoolantCellBlock(machine(), ReactorBlockEntities.COOLANT_CELL));

    public static final DeferredBlock<CpuBlock> CPU = block("cpu",
            () -> new CpuBlock(machine(), ReactorBlockEntities.CPU));

    public static final DeferredBlock<BoilerBlock> REACTOR_BOILER = block("reactor_boiler",
            () -> new BoilerBlock(machine(), ReactorBlockEntities.REACTOR_BOILER));
    public static final DeferredBlock<SteamLineBlock> STEAM_LINE = block("steam_line",
            () -> new SteamLineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0F, 1F).sound(SoundType.WOOL).noOcclusion(),
                    ReactorBlockEntities.STEAM_LINE));
    public static final DeferredBlock<SteamGrateBlock> STEAM_GRATE = block("steam_grate",
            () -> new SteamGrateBlock(machine().noOcclusion(), ReactorBlockEntities.STEAM_GRATE));
    public static final DeferredBlock<SteamBlock> STEAM = block("steam",
            () -> new SteamBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).noCollission().noOcclusion().replaceable().strength(0F, 3600000F)
                    .noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final DeferredBlock<ReactorMachineBlock> CONDENSER = block("condenser",
            () -> new ReactorMachineBlock(machine().noOcclusion(), ReactorBlockEntities.CONDENSER, true, false));
    public static final DeferredBlock<ReactorMachineBlock> REACTOR_PUMP = block("reactor_pump",
            () -> new ReactorMachineBlock(machine().noOcclusion(), ReactorBlockEntities.REACTOR_PUMP, true, true));

    public static final DeferredBlock<TurbineCoreBlock> TURBINE_CORE = block("turbine_core",
            () -> new TurbineCoreBlock(machine().noOcclusion(), ReactorBlockEntities.TURBINE_CORE));

    public static final DeferredBlock<TurbineMeterBlock> TURBINE_METER = block("turbine_meter",
            () -> new TurbineMeterBlock(machine(), ReactorBlockEntities.TURBINE_METER));

    public static final DeferredBlock<ReactorMachineBlock> REFLECTOR = block("neutron_reflector",
            () -> new ReactorMachineBlock(machine(), ReactorBlockEntities.REFLECTOR, false, false));
    public static final DeferredBlock<ReactorMachineBlock> ABSORBER = block("neutron_absorber",
            () -> new ReactorMachineBlock(machine(), ReactorBlockEntities.ABSORBER, false, false));

    public static final DeferredBlock<StackableMachineBlock> BREEDER_CORE = block("breeder_core",
            () -> new StackableMachineBlock(machine(), ReactorBlockEntities.BREEDER_CORE));
    public static final DeferredBlock<StackableMachineBlock> SODIUM_HEATER = block("sodium_heater",
            () -> new StackableMachineBlock(machine(), ReactorBlockEntities.SODIUM_HEATER));

    private ReactorBlocks() {
    }

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2F, 10F).sound(SoundType.METAL).requiresCorrectToolForDrops();
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
