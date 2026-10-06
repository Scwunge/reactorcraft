package net.scwunge.reactorcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.waste.WasteManager;

public final class ReactorTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ReactorCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.reactorcraft"))
            .icon(() -> ReactorItems.URANIUM_INGOT.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (DeferredItem<? extends Item> item : ReactorItems.TAB) {
                    output.accept(item.get());
                }
                for (ItemStack waste : WasteManager.creativeStacks()) {
                    output.accept(waste);
                }
            })
            .build());

    private ReactorTabs() {
    }
}
