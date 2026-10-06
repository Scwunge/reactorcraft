package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** The automation view of a machine inventory from one side: only the slots and moves the machine allows from there. */
public class SidedItemHandler implements IItemHandler {
    private final ReactorMachineBlockEntity machine;
    private final Direction side;

    public SidedItemHandler(ReactorMachineBlockEntity machine, Direction side) {
        this.machine = machine;
        this.side = side;
    }

    private int[] slots() {
        return machine.slotsForFace(side);
    }

    private IItemHandlerModifiable inner() {
        return machine.items();
    }

    @Override
    public int getSlots() {
        return slots().length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int[] slots = slots();
        return slot < slots.length ? inner().getStackInSlot(slots[slot]) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int[] slots = slots();
        if (slot >= slots.length || !machine.canInsertFromSide(slots[slot], stack, side)) {
            return stack;
        }
        return inner().insertItem(slots[slot], stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int[] slots = slots();
        if (slot >= slots.length || !machine.canExtractFromSide(slots[slot], side)) {
            return ItemStack.EMPTY;
        }
        return machine.extractForAutomation(slots[slot], amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        int[] slots = slots();
        return slot < slots.length ? inner().getSlotLimit(slots[slot]) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        int[] slots = slots();
        return slot < slots.length && machine.canInsertFromSide(slots[slot], stack, side);
    }
}
