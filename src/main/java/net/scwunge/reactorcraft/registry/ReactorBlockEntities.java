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
