package net.scwunge.reactorcraft.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.client.model.ModelCentrifuge;
import net.scwunge.reactorcraft.client.model.ModelElectrolyzer;
import net.scwunge.reactorcraft.client.model.ModelCondenser;
import net.scwunge.reactorcraft.client.model.ModelGasCollector;
import net.scwunge.reactorcraft.client.model.ModelReactorPump;
import net.scwunge.reactorcraft.client.model.ModelSteamGrate;
import net.scwunge.reactorcraft.client.model.ModelHeavyPump;
import net.scwunge.reactorcraft.client.model.ModelProcessor;
import net.scwunge.reactorcraft.client.model.ModelWasteStorage;
import net.scwunge.reactorcraft.client.model.ModelControl;
import net.scwunge.reactorcraft.client.render.ControlRodRenderer;
import net.scwunge.reactorcraft.client.render.GasCollectorRenderer;
import net.scwunge.reactorcraft.client.render.MachineItemRenderer;
import net.scwunge.reactorcraft.client.render.ModelMachineRenderer;
import net.scwunge.reactorcraft.client.render.NeutronRenderer;
import net.scwunge.reactorcraft.client.render.ProcessorRenderer;
import net.scwunge.reactorcraft.client.render.TurbineRenderer;
import net.scwunge.reactorcraft.client.screen.CpuScreen;
import net.scwunge.reactorcraft.client.screen.ReactorMachineScreen;
import net.scwunge.reactorcraft.content.machine.CpuBlockEntity;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.reactorcraft.registry.ReactorItems;
import net.scwunge.reactorcraft.content.machine.ReactorMenu;
import net.scwunge.reactorcraft.registry.ReactorMenus;

@EventBusSubscriber(modid = ReactorCraft.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private static final ModelLayerLocation FLUID_EXTRACTOR = layer("fluid_extractor");
    private static final ModelLayerLocation ISOTOPE_CENTRIFUGE = layer("isotope_centrifuge");
    private static final ModelLayerLocation URANIUM_PROCESSOR = layer("uranium_processor");
    private static final ModelLayerLocation ELECTROLYZER = layer("electrolyzer");
    private static final ModelLayerLocation GAS_COLLECTOR = layer("gas_collector");
    private static final ModelLayerLocation WASTE_STORAGE = layer("waste_storage");
    private static final ModelLayerLocation CONTROL_ROD = layer("control_rod");
    private static final ModelLayerLocation STEAM_GRATE = layer("steam_grate");
    private static final ModelLayerLocation CONDENSER = layer("condenser");
    private static final ModelLayerLocation REACTOR_PUMP = layer("reactor_pump");
    private static final ModelLayerLocation[] TURBINE_STAGES = new ModelLayerLocation[TurbineRenderer.STAGES];

    static {
        for (int i = 0; i < TURBINE_STAGES.length; i++) {
            TURBINE_STAGES[i] = layer("turbine_stage_" + i);
        }
    }

    private ClientSetup() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ReactorCraft.id(name), "main");
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FLUID_EXTRACTOR, ModelHeavyPump::create);
        event.registerLayerDefinition(ISOTOPE_CENTRIFUGE, ModelCentrifuge::create);
        event.registerLayerDefinition(URANIUM_PROCESSOR, ModelProcessor::create);
        event.registerLayerDefinition(ELECTROLYZER, ModelElectrolyzer::create);
        event.registerLayerDefinition(GAS_COLLECTOR, ModelGasCollector::create);
        event.registerLayerDefinition(WASTE_STORAGE, ModelWasteStorage::create);
        event.registerLayerDefinition(CONTROL_ROD, ModelControl::create);
        event.registerLayerDefinition(STEAM_GRATE, ModelSteamGrate::create);
        event.registerLayerDefinition(CONDENSER, ModelCondenser::create);
        event.registerLayerDefinition(REACTOR_PUMP, ModelReactorPump::create);
        for (int i = 0; i < TURBINE_STAGES.length; i++) {
            int stage = i;
            event.registerLayerDefinition(TURBINE_STAGES[i], () -> TurbineRenderer.createLayer(stage));
        }
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ReactorBlockEntities.FLUID_EXTRACTOR.get(), context ->
                new ModelMachineRenderer<>(context, FLUID_EXTRACTOR, "fluid_extractor", ModelHeavyPump.PARTS,
                        "shape3", "shape3a", "shape3b", "shape3c", "shape3d", "shape3e"));
        event.registerBlockEntityRenderer(ReactorBlockEntities.ISOTOPE_CENTRIFUGE.get(), context ->
                new ModelMachineRenderer<>(context, ISOTOPE_CENTRIFUGE, "isotope_centrifuge", ModelCentrifuge.PARTS,
                        "shape2", "shape2a", "shape2b", "shape2c", "shape2d", "shape2e", "shape2f", "shape2g",
                        "shape3", "shape3a", "shape3b", "shape3c"));
        event.registerBlockEntityRenderer(ReactorBlockEntities.URANIUM_PROCESSOR.get(), context -> new ProcessorRenderer(context, URANIUM_PROCESSOR));
        event.registerBlockEntityRenderer(ReactorBlockEntities.ELECTROLYZER.get(), context ->
                new ModelMachineRenderer<>(context, ELECTROLYZER, "electrolyzer", ModelElectrolyzer.PARTS));
        event.registerBlockEntityRenderer(ReactorBlockEntities.GAS_COLLECTOR.get(), context -> new GasCollectorRenderer(context, GAS_COLLECTOR));
        event.registerBlockEntityRenderer(ReactorBlockEntities.CONTROL_ROD.get(), context -> new ControlRodRenderer(context, CONTROL_ROD));
        event.registerBlockEntityRenderer(ReactorBlockEntities.STEAM_GRATE.get(), context ->
                new ModelMachineRenderer<>(context, STEAM_GRATE, "steam_grate", ModelSteamGrate.PARTS));
        event.registerBlockEntityRenderer(ReactorBlockEntities.CONDENSER.get(), context ->
                new ModelMachineRenderer<>(context, CONDENSER, "condenser", ModelCondenser.PARTS));
        event.registerBlockEntityRenderer(ReactorBlockEntities.REACTOR_PUMP.get(), context ->
                new ModelMachineRenderer<>(context, REACTOR_PUMP, "reactor_pump", ModelReactorPump.PARTS,
                        java.util.Arrays.stream(ModelReactorPump.PARTS).filter(n -> n.startsWith("shape2") || n.startsWith("shape3")).toArray(String[]::new)));
        event.registerBlockEntityRenderer(ReactorBlockEntities.TURBINE_CORE.get(), context -> new TurbineRenderer(context, TURBINE_STAGES, "turbine_core"));
        event.registerBlockEntityRenderer(ReactorBlockEntities.WASTE_STORAGE.get(), context ->
                new ModelMachineRenderer<>(context, WASTE_STORAGE, "waste_storage", ModelWasteStorage.PARTS));
    }

    @SubscribeEvent
    static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ReactorEntities.NEUTRON.get(), NeutronRenderer::new);
    }

    private static ReactorMachineScreen machineScreen(ReactorMenu menu, net.minecraft.world.entity.player.Inventory inventory,
                                                      net.minecraft.network.chat.Component title) {
        return menu.machine() instanceof CpuBlockEntity ? new CpuScreen(menu, inventory, title) : new ReactorMachineScreen(menu, inventory, title);
    }

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(ReactorMenus.MACHINE.get(), ClientSetup::machineScreen);
    }

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        // mixed waste has its own sprite
        event.enqueueWork(() -> ItemProperties.register(ReactorItems.NUCLEAR_WASTE.get(), ReactorCraft.id("mixed"),
                (stack, level, entity, seed) -> WasteManager.isMixed(stack) ? 1F : 0F));
    }

    @SubscribeEvent
    static void clientExtensions(RegisterClientExtensionsEvent event) {
        // the modelled machines are drawn as items by their block entity renderers
        event.registerItem(new MachineItemRenderer(), ReactorBlocks.FLUID_EXTRACTOR.asItem(), ReactorBlocks.ISOTOPE_CENTRIFUGE.asItem(),
                ReactorBlocks.URANIUM_PROCESSOR.asItem(), ReactorBlocks.ELECTROLYZER.asItem(), ReactorBlocks.GAS_COLLECTOR.asItem(),
                ReactorBlocks.CONTROL_ROD.asItem(), ReactorBlocks.STEAM_GRATE.asItem(), ReactorBlocks.CONDENSER.asItem(), ReactorBlocks.REACTOR_PUMP.asItem(), ReactorBlocks.TURBINE_CORE.asItem(),
                ReactorBlocks.WASTE_STORAGE.asItem());
        for (ReactorFluids.Entry fluid : ReactorFluids.ALL) {
            ResourceLocation still = fluid.texture == null
                    ? ResourceLocation.withDefaultNamespace("block/water_still") : ReactorCraft.id("block/fluid/" + fluid.texture);
            ResourceLocation flowing = fluid.texture == null
                    ? ResourceLocation.withDefaultNamespace("block/water_flow") : still;
            int tint = fluid.texture == null ? 0xFF3F76E4 : 0xFFFFFFFF;
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return still;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flowing;
                }

                @Override
                public int getTintColor() {
                    return tint;
                }
            }, fluid.type.get());
        }
    }
}
