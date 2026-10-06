package net.scwunge.reactorcraft.content.machine;

import net.minecraft.world.item.ItemStack;

/** A waste unit that stacked units above it can pass waste down into (the original's Feedable). */
public interface Feedable {
    /** Takes {@code stack} into the top slot if it is empty and the stack is valid there; an empty stack is always "taken". */
    boolean feedIn(ItemStack stack);

    /** Hands over the bottom slot's stack, emptying it. */
    ItemStack feedOut();
}
