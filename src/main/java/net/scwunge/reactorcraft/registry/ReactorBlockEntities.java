package net.scwunge.reactorcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.ElectrolyzerBlockEntity;
import net.scwunge.reactorcraft.content.machine.FluidExtractorBlockEntity;
import net.scwunge.reactorcraft.content.machine.FluidSynthesizerBlockEntity;
import net.scwunge.reactorcraft.content.machine.GasCollectorBlockEntity;
import net.scwunge.reactorcraft.content.machine.IsotopeCentrifugeBlockEntity;
import net.scwunge.reactorcraft.content.machine.UraniumProcessorBlockEntity;
import net.scwunge.reactorcraft.content.machine.FuelRodBlockEntity;
import net.scwunge.reactorcraft.content.machine.ControlRodBlockEntity;
import net.scwunge.reactorcraft.content.machine.CoolantCellBlockEntity;
import net.scwunge.reactorcraft.content.machine.CondenserBlockEntity;
import net.scwunge.reactorcraft.content.machine.CpuBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorPumpBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorBoilerBlockEntity;
import net.scwunge.reactorcraft.content.machine.SteamGrateBlockEntity;
import net.scwunge.reactorcraft.content.machine.TurbineCoreBlockEntity;
import net.scwunge.reactorcraft.content.machine.TurbineMeterBlockEntity;
import net.scwunge.reactorcraft.content.machine.SteamLineBlockEntity;
import net.scwunge.reactorcraft.content.machine.WasteContainerBlockEntity;
import net.scwunge.reactorcraft.content.machine.WasteDecayerBlockEntity;
import net.scwunge.reactorcraft.content.machine.WasteStorageBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ReactorBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ReactorCraft.MODID);
    /** Every machine type, for capability registration. */
    public static final List<DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>>> ALL = new ArrayList<>();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidExtractorBlockEntity>> FLUID_EXTRACTOR =
            register("fluid_extractor", FluidExtractorBlockEntity::new, ReactorBlocks.FLUID_EXTRACTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IsotopeCentrifugeBlockEntity>> ISOTOPE_CENTRIFUGE =
            register("isotope_centrifuge", IsotopeCentrifugeBlockEntity::new, ReactorBlocks.ISOTOPE_CENTRIFUGE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UraniumProcessorBlockEntity>> URANIUM_PROCESSOR =
            register("uranium_processor", UraniumProcessorBlockEntity::new, ReactorBlocks.URANIUM_PROCESSOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER =
            register("electrolyzer", ElectrolyzerBlockEntity::new, ReactorBlocks.ELECTROLYZER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidSynthesizerBlockEntity>> FLUID_SYNTHESIZER =
            register("fluid_synthesizer", FluidSynthesizerBlockEntity::new, ReactorBlocks.FLUID_SYNTHESIZER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasCollectorBlockEntity>> GAS_COLLECTOR =
            register("gas_collector", GasCollectorBlockEntity::new, ReactorBlocks.GAS_COLLECTOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteContainerBlockEntity>> WASTE_CONTAINER =
            register("waste_container", WasteContainerBlockEntity::new, ReactorBlocks.WASTE_CONTAINER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteStorageBlockEntity>> WASTE_STORAGE =
            register("waste_storage", WasteStorageBlockEntity::new, ReactorBlocks.WASTE_STORAGE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteDecayerBlockEntity>> WASTE_DECAYER =
            register("waste_decayer", WasteDecayerBlockEntity::new, ReactorBlocks.WASTE_DECAYER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelRodBlockEntity>> FUEL_ROD =
            register("fuel_rod", FuelRodBlockEntity::new, ReactorBlocks.FUEL_ROD);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControlRodBlockEntity>> CONTROL_ROD =
            register("control_rod", ControlRodBlockEntity::new, ReactorBlocks.CONTROL_ROD);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoolantCellBlockEntity>> COOLANT_CELL =
            register("coolant_cell", CoolantCellBlockEntity::new, ReactorBlocks.COOLANT_CELL);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CpuBlockEntity>> CPU =
            register("cpu", CpuBlockEntity::new, ReactorBlocks.CPU);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorBoilerBlockEntity>> REACTOR_BOILER =
            register("reactor_boiler", ReactorBoilerBlockEntity::new, ReactorBlocks.REACTOR_BOILER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamLineBlockEntity>> STEAM_LINE =
            register("steam_line", SteamLineBlockEntity::new, ReactorBlocks.STEAM_LINE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamGrateBlockEntity>> STEAM_GRATE =
            register("steam_grate", SteamGrateBlockEntity::new, ReactorBlocks.STEAM_GRATE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserBlockEntity>> CONDENSER =
            register("condenser", CondenserBlockEntity::new, ReactorBlocks.CONDENSER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorPumpBlockEntity>> REACTOR_PUMP =
            register("reactor_pump", ReactorPumpBlockEntity::new, ReactorBlocks.REACTOR_PUMP);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineCoreBlockEntity>> TURBINE_CORE =
            register("turbine_core", TurbineCoreBlockEntity::new, ReactorBlocks.TURBINE_CORE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineMeterBlockEntity>> TURBINE_METER =
            register("turbine_meter", TurbineMeterBlockEntity::new, ReactorBlocks.TURBINE_METER);

    private ReactorBlockEntities() {
    }

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> holder = TYPES.register(name,
                () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        ALL.add(holder);
        return holder;
    }
}
