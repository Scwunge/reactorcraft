package net.scwunge.reactorcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.ReactorMenu;

public final class ReactorMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ReactorCraft.MODID);

    /** Every machine GUI shares this menu type; the client picks the screen by machine. */
    public static final DeferredHolder<MenuType<?>, MenuType<ReactorMenu>> MACHINE = MENUS.register("machine",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new ReactorMenu(ReactorMenus.MACHINE.get(), id, inventory, buf)));

    private ReactorMenus() {
    }
}
