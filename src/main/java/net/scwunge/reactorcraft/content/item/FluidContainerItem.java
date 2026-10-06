package net.scwunge.reactorcraft.content.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A canister (or the heavy water bucket): always either empty or holding exactly 1000 mB of one fluid, like the original's
 * fluid container registrations. Filling an empty one swaps it for the matching full item; draining a full one swaps it
 * back.
 */
public class FluidContainerItem extends Item {
    public static final int VOLUME = 1000;
    /** Full container for each fluid, in registration order. */
    private static final Map<Supplier<? extends Fluid>, Supplier<? extends Item>> FULL = new LinkedHashMap<>();

    @Nullable
    private final Supplier<? extends Fluid> fluid;
    private final Supplier<? extends Item> empty;

    /** A full container of {@code fluid} that empties into {@code empty}. */
    public FluidContainerItem(Supplier<? extends Fluid> fluid, Supplier<? extends Item> empty, Properties properties) {
        super(properties);
        this.fluid = fluid;
        this.empty = empty;
    }

    /** The empty container. */
    public FluidContainerItem(Properties properties) {
        super(properties);
        this.fluid = null;
        this.empty = () -> this;
    }

    public static void register(Supplier<? extends Fluid> fluid, Supplier<? extends Item> full) {
        FULL.put(fluid, full);
    }

    @Nullable
    public Fluid fluid() {
        return fluid == null ? null : fluid.get();
    }

    @Nullable
    private static Item fullFor(Fluid fluid) {
        for (Map.Entry<Supplier<? extends Fluid>, Supplier<? extends Item>> e : FULL.entrySet()) {
            if (e.getKey().get() == fluid) {
                return e.getValue().get();
            }
        }
        return null;
    }

    /** The capability for a stack of this item. */
    public IFluidHandlerItem handler(ItemStack stack) {
        return new Handler(stack);
    }

    private final class Handler implements IFluidHandlerItem {
        private ItemStack container;

        Handler(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        private FluidStack contents() {
            return container.getItem() instanceof FluidContainerItem item && item.fluid() != null
                    ? new FluidStack(item.fluid(), VOLUME) : FluidStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return contents();
        }

        @Override
        public int getTankCapacity(int tank) {
            return VOLUME;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return fullFor(stack.getFluid()) != null;
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (container.getCount() != 1 || resource.getAmount() < VOLUME || !contents().isEmpty()) {
                return 0;
            }
            Item full = fullFor(resource.getFluid());
            if (full == null) {
                return 0;
            }
            if (action.execute()) {
                container = new ItemStack(full);
            }
            return VOLUME;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            FluidStack in = contents();
            if (in.isEmpty() || !FluidStack.isSameFluidSameComponents(in, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            FluidStack in = contents();
            if (container.getCount() != 1 || in.isEmpty() || maxDrain < VOLUME) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                container = new ItemStack(empty.get());
            }
            return in;
        }
    }
}
