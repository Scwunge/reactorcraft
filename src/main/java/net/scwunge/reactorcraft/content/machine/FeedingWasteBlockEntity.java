package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Waste units of one kind stacked in a column pass their contents down: each tick the bottom slot is offered to the unit
 * below, and if it takes it everything shifts down a slot and the top slot is refilled from the unit above.
 */
public abstract class FeedingWasteBlockEntity extends ReactorMachineBlockEntity implements Feedable {
    protected FeedingWasteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    protected final int size() {
        return items().getSlots();
    }

    /** feed: one step of passing waste down the column, then tidying this unit. */
    protected void feed() {
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (getClass().isInstance(below) && ((Feedable) below).feedIn(stack(size() - 1))) {
            for (int i = size() - 1; i > 0; i--) {
                setStack(i, stack(i - 1).copy());
            }
            BlockEntity above = level.getBlockEntity(worldPosition.above());
            setStack(0, getClass().isInstance(above) ? ((Feedable) above).feedOut() : ItemStack.EMPTY);
        }
        collapseInventory();
    }

    /** Moves waste towards the bottom of this unit. */
    protected abstract void collapseInventory();

    @Override
    public boolean feedIn(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (!isItemValid(0, stack)) {
            return false;
        }
        if (stack(0).isEmpty()) {
            setStack(0, stack.copy());
            return true;
        }
        return false;
    }

    @Override
    public ItemStack feedOut() {
        ItemStack last = stack(size() - 1);
        if (last.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = last.copy();
        setStack(size() - 1, ItemStack.EMPTY);
        return out;
    }
}
