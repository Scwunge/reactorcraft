package net.scwunge.reactorcraft.content.machine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** What the original's ItemMatch and FlexibleIngredient did: an item is accepted if it has one of the tags or is one of the items. */
public final class ItemMatch {
    private final List<TagKey<Item>> tags = new ArrayList<>();
    private final List<Supplier<? extends Item>> items = new ArrayList<>();

    private ItemMatch() {
    }

    public static ItemMatch of() {
        return new ItemMatch();
    }

    /** Adds a common tag, e.g. {@code "c:dusts/salt"}. */
    public ItemMatch tag(String id) {
        tags.add(TagKey.create(Registries.ITEM, ResourceLocation.parse(id)));
        return this;
    }

    public ItemMatch item(Supplier<? extends Item> item) {
        items.add(item);
        return this;
    }

    /** Example stacks that are accepted: each listed item, and the items under each tag (for recipe viewers). */
    public List<ItemStack> examples() {
        List<ItemStack> list = new ArrayList<>();
        for (Supplier<? extends Item> item : items) {
            list.add(new ItemStack(item.get()));
        }
        for (TagKey<Item> tag : tags) {
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
                ItemStack stack = new ItemStack(holder.value());
                if (list.stream().noneMatch(other -> ItemStack.isSameItem(other, stack))) {
                    list.add(stack);
                }
            });
        }
        return list;
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        for (Supplier<? extends Item> item : items) {
            if (stack.is(item.get())) {
                return true;
            }
        }
        return false;
    }
}
