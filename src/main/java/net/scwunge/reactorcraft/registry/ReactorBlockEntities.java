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
import net.scwunge.reactorcraft.content.machine.AbsorberBlockEntity;
import net.scwunge.reactorcraft.content.machine.BreederCoreBlockEntity;
import net.scwunge.reactorcraft.content.machine.Co2HeaterBlockEntity;
import net.scwunge.reactorcraft.content.machine.CpuBlockEntity;
import net.scwunge.reactorcraft.content.machine.HeatExchangerBlockEntity;
import net.scwunge.reactorcraft.content.machine.FusionInjectorBlockEntity;
import net.scwunge.reactorcraft.content.machine.GasDuctBlockEntity;
import net.scwunge.reactorcraft.content.machine.CentrifugalTurbineBlockEntity;
import net.scwunge.reactorcraft.content.machine.FusionHeaterBlockEntity;
import net.scwunge.reactorcraft.content.machine.SteamDiffuserBlockEntity;
import net.scwunge.reactorcraft.content.machine.FusionMarkerBlockEntity;
import net.scwunge.reactorcraft.content.machine.TritizerBlockEntity;
import net.scwunge.reactorcraft.content.machine.HeatPipeBlockEntity;
import net.scwunge.reactorcraft.content.machine.SolenoidBlockEntity;
import net.scwunge.reactorcraft.content.machine.MagneticPipeBlockEntity;
import net.scwunge.reactorcraft.content.machine.ToroidMagnetBlockEntity;
import net.scwunge.reactorcraft.content.machine.PebbleBedBlockEntity;
import net.scwunge.reactorcraft.content.machine.FuelDumpBlockEntity;
import net.scwunge.reactorcraft.content.machine.ThoriumCoreBlockEntity;
import net.scwunge.reactorcraft.content.machine.SodiumHeaterBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReflectorBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReflectorBlockEntity>> REFLECTOR =
            register("neutron_reflector", ReflectorBlockEntity::new, ReactorBlocks.REFLECTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AbsorberBlockEntity>> ABSORBER =
            register("neutron_absorber", AbsorberBlockEntity::new, ReactorBlocks.ABSORBER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreederCoreBlockEntity>> BREEDER_CORE =
            register("breeder_core", BreederCoreBlockEntity::new, ReactorBlocks.BREEDER_CORE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SodiumHeaterBlockEntity>> SODIUM_HEATER =
            register("sodium_heater", SodiumHeaterBlockEntity::new, ReactorBlocks.SODIUM_HEATER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThoriumCoreBlockEntity>> THORIUM_CORE =
            register("thorium_core", ThoriumCoreBlockEntity::new, ReactorBlocks.THORIUM_CORE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelDumpBlockEntity>> FUEL_DUMP =
            register("fuel_dump", FuelDumpBlockEntity::new, ReactorBlocks.FUEL_DUMP);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PebbleBedBlockEntity>> PEBBLE_BED =
            register("pebble_bed", PebbleBedBlockEntity::new, ReactorBlocks.PEBBLE_BED);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Co2HeaterBlockEntity>> CO2_HEATER =
            register("co2_heater", Co2HeaterBlockEntity::new, ReactorBlocks.CO2_HEATER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatExchangerBlockEntity>> HEAT_EXCHANGER =
            register("heat_exchanger", HeatExchangerBlockEntity::new, ReactorBlocks.HEAT_EXCHANGER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatPipeBlockEntity>> HEAT_PIPE =
            register("heat_pipe", HeatPipeBlockEntity::new, ReactorBlocks.HEAT_PIPE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToroidMagnetBlockEntity>> TOROID_MAGNET =
            register("toroid_magnet", ToroidMagnetBlockEntity::new, ReactorBlocks.TOROID_MAGNET);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionInjectorBlockEntity>> FUSION_INJECTOR =
            register("fusion_injector", FusionInjectorBlockEntity::new, ReactorBlocks.FUSION_INJECTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagneticPipeBlockEntity>> MAGNETIC_PIPE =
            register("magnetic_pipe", MagneticPipeBlockEntity::new, ReactorBlocks.MAGNETIC_PIPE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasDuctBlockEntity>> GAS_DUCT =
            register("gas_duct", GasDuctBlockEntity::new, ReactorBlocks.GAS_DUCT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolenoidBlockEntity>> SOLENOID =
            register("solenoid_magnet", SolenoidBlockEntity::new, ReactorBlocks.SOLENOID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionHeaterBlockEntity>> FUSION_HEATER =
            register("fusion_heater", FusionHeaterBlockEntity::new, ReactorBlocks.FUSION_HEATER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TritizerBlockEntity>> TRITIZER =
            register("tritizer", TritizerBlockEntity::new, ReactorBlocks.TRITIZER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionMarkerBlockEntity>> FUSION_MARKER =
            register("fusion_marker", FusionMarkerBlockEntity::new, ReactorBlocks.FUSION_MARKER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamDiffuserBlockEntity>> STEAM_DIFFUSER =
            register("steam_diffuser", SteamDiffuserBlockEntity::new, ReactorBlocks.STEAM_DIFFUSER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugalTurbineBlockEntity>> CENTRIFUGAL_TURBINE =
            register("centrifugal_turbine", CentrifugalTurbineBlockEntity::new, ReactorBlocks.CENTRIFUGAL_TURBINE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.reactorcraft.content.machine.BigTurbineBlockEntity>> BIG_TURBINE =
            register("big_turbine", net.scwunge.reactorcraft.content.machine.BigTurbineBlockEntity::new, ReactorBlocks.BIG_TURBINE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.reactorcraft.content.machine.SteamInjectorBlockEntity>> STEAM_INJECTOR =
            register("steam_injector", net.scwunge.reactorcraft.content.machine.SteamInjectorBlockEntity::new, () -> ReactorBlocks.TURBINE_PARTS.get(2).get());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.reactorcraft.content.machine.ReactorFlywheelBlockEntity>> FLYWHEEL =
            register("flywheel", net.scwunge.reactorcraft.content.machine.ReactorFlywheelBlockEntity::new, ReactorBlocks.FLYWHEEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.reactorcraft.content.machine.ReactorGeneratorBlockEntity>> GENERATOR =
            register("generator", net.scwunge.reactorcraft.content.machine.ReactorGeneratorBlockEntity::new, ReactorBlocks.GENERATOR);

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
