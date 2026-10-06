package net.scwunge.reactorcraft;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorMenus;
import net.scwunge.reactorcraft.registry.ReactorFeatures;
import net.scwunge.reactorcraft.registry.ReactorComponents;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.reactorcraft.registry.ReactorItems;
import net.scwunge.reactorcraft.registry.ReactorTabs;
import org.slf4j.Logger;

/** ReactorCraft: nuclear power for RotaryCraft. A 1.21.1 port of Reika Kalseki's mod. */
@Mod(ReactorCraft.MODID)
public final class ReactorCraft {
    public static final String MODID = "reactorcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ReactorCraft(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ReactorConfig.SPEC);
        ReactorFluids.TYPES.register(modBus);
        ReactorFluids.FLUIDS.register(modBus);
        ReactorBlocks.BLOCKS.register(modBus);
        ReactorComponents.COMPONENTS.register(modBus);
        ReactorItems.ITEMS.register(modBus);
        ReactorBlockEntities.TYPES.register(modBus);
        ReactorMenus.MENUS.register(modBus);
        ReactorTabs.TABS.register(modBus);
        ReactorFeatures.FEATURES.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
