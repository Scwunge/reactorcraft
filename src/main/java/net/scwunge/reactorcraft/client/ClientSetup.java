package net.scwunge.reactorcraft.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.registry.ReactorFluids;

@EventBusSubscriber(modid = ReactorCraft.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    static void clientExtensions(RegisterClientExtensionsEvent event) {
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
