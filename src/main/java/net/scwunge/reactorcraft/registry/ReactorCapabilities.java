package net.scwunge.reactorcraft.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.item.FluidContainerItem;

@EventBusSubscriber(modid = ReactorCraft.MODID)
public final class ReactorCapabilities {
    private ReactorCapabilities() {
    }

    @SubscribeEvent
    static void register(RegisterCapabilitiesEvent event) {
        for (DeferredHolder<Item, ? extends Item> holder : ReactorItems.ITEMS.getEntries()) {
            if (holder.get() instanceof FluidContainerItem container) {
                event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> container.handler(stack), container);
            }
        }
        for (DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>> holder : ReactorBlockEntities.ALL) {
            @SuppressWarnings("unchecked")
            BlockEntityType<ReactorMachineBlockEntity> type = (BlockEntityType<ReactorMachineBlockEntity>) holder.get();
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, ReactorMachineBlockEntity::fluidHandler);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, ReactorMachineBlockEntity::itemHandler);
        }
    }
}
