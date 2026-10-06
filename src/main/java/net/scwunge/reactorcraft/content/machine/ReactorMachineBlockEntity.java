package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;
import net.scwunge.reactorcraft.registry.ReactorMenus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A ReactorCraft machine (the original's TileEntityInventoriedReactorBase and its tanked relatives): an item inventory
 * with per-slot and per-side rules, named fluid tanks that are saved, synced to the client and shown in the GUI, a spin
 * angle for animated models, and a menu.
 */
public abstract class ReactorMachineBlockEntity extends ReactorBlockEntity implements MenuProvider {
    private final ItemStackHandler items;
    private final Map<Direction, IItemHandler> sidedHandlers = new EnumMap<>(Direction.class);
    private final Map<String, FluidTank> tanks = new LinkedHashMap<>();
    private final ContainerData dataAccess;

    /** Model spin angle in degrees (the original's phi); advanced on the client. */
    public float phi;
    public float prevPhi;

    protected ReactorMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        this.items = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return ReactorMachineBlockEntity.this.isItemValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotLimit(slot);
            }

            @Override
            protected void onContentsChanged(int slot) {
                onInventoryChanged(slot);
            }
        };
        this.dataAccess = new ContainerData() {
            // every int goes as two shorts, since container data travels as 16-bit values
            @Override
            public int get(int index) {
                int value = rawGuiValue(index / 2);
                return index % 2 == 0 ? value & 0xFFFF : value >>> 16;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return (tanks.size() * 2 + guiValueCount()) * 2;
            }
        };
    }

    // ---- tanks ----

    /** Adds a tank, saved under {@code name}, that takes only what {@code valid} accepts. Call from the constructor. */
    protected FluidTank addTank(String name, int capacity, Predicate<FluidStack> valid) {
        FluidTank tank = new FluidTank(capacity, valid) {
            @Override
            protected void onContentsChanged() {
                markForSync();
            }
        };
        tanks.put(name, tank);
        return tank;
    }

    /** The tanks in the order they were added: the GUI's tank indices. */
    public List<FluidTank> tanks() {
        return new ArrayList<>(tanks.values());
    }

    /** The fluid handler pipes see on {@code side} (null: the block itself, as for buckets). Null for none. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return null;
    }

    /** Adds {@code amount} of {@code fluid} to a tank, ignoring its filter (the original's HybridTank.addLiquid). */
    protected static void addLiquid(FluidTank tank, Fluid fluid, int amount) {
        if (tank.isEmpty()) {
            tank.setFluid(new FluidStack(fluid, Math.min(amount, tank.getCapacity())));
        } else if (tank.getFluid().is(fluid)) {
            tank.setFluid(tank.getFluid().copyWithAmount(Math.min(tank.getCapacity(), tank.getFluidAmount() + amount)));
        }
    }

    /** Removes {@code amount} from a tank (HybridTank.removeLiquid). */
    protected static void removeLiquid(FluidTank tank, int amount) {
        tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
    }

    /** HybridTank.canTakeIn: the tank is empty or holds {@code fluid}, and {@code amount} more fits. */
    protected static boolean canTakeIn(FluidTank tank, Fluid fluid, int amount) {
        return (tank.isEmpty() || tank.getFluid().is(fluid)) && tank.getFluidAmount() + amount <= tank.getCapacity();
    }

    protected static Fluid fluidOf(FluidTank tank) {
        return tank.isEmpty() ? Fluids.EMPTY : tank.getFluid().getFluid();
    }

    // ---- inventory rules ----

    public IItemHandlerModifiable items() {
        return items;
    }

    /** Whether a player or automation may put this stack in this slot. Output slots return false. */
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    protected int slotLimit(int slot) {
        return 64;
    }

    /** Slots reachable from a side by hoppers and pipes; by default all of them, as in the original. */
    public int[] slotsForFace(@Nullable Direction side) {
        int[] all = new int[items.getSlots()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        return all;
    }

    public boolean canInsertFromSide(int slot, ItemStack stack, @Nullable Direction side) {
        return isItemValid(slot, stack);
    }

    /** The original's canRemoveItem && canItemExitToSide. */
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return false;
    }

    public ItemStack extractForAutomation(int slot, int amount, boolean simulate) {
        return items.extractItem(slot, amount, simulate);
    }

    @Nullable
    public IItemHandler itemHandler(@Nullable Direction side) {
        if (items.getSlots() == 0) {
            return null;
        }
        if (side == null) {
            return items;
        }
        return sidedHandlers.computeIfAbsent(side, s -> new SidedItemHandler(this, s));
    }

    protected void onInventoryChanged(int slot) {
        setChanged();
    }

    protected ItemStack stack(int slot) {
        return items.getStackInSlot(slot);
    }

    protected void setStack(int slot, ItemStack stack) {
        items.setStackInSlot(slot, stack);
    }

    /** ReikaInventoryHelper.decrStack. */
    protected void shrink(int slot, int count) {
        ItemStack stack = items.getStackInSlot(slot).copy();
        stack.shrink(count);
        items.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
    }

    /** Whether {@code result} fits in a slot (ignores slot rules). */
    protected boolean canOutput(int slot, ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }
        ItemStack existing = items.getStackInSlot(slot);
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(existing, result)
                && existing.getCount() + result.getCount() <= Math.min(existing.getMaxStackSize(), items.getSlotLimit(slot));
    }

    /** ReikaInventoryHelper.addOrSetStack: puts {@code result} into a slot (ignores slot rules). Check {@link #canOutput} first. */
    protected void output(int slot, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        ItemStack existing = items.getStackInSlot(slot);
        if (existing.isEmpty()) {
            items.setStackInSlot(slot, result.copy());
        } else {
            ItemStack grown = existing.copy();
            grown.grow(result.getCount());
            items.setStackInSlot(slot, grown);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
        }
    }

    // ---- animation ----

    /** Advances the model's spin by {@code degrees} this tick. Call from {@link #tickClient}. */
    protected void spin(float degrees) {
        prevPhi = phi;
        phi += degrees;
        if (phi >= 360000) {
            phi -= 360000;
            prevPhi -= 360000;
        }
    }

    public float phi(float partialTick) {
        return prevPhi + (phi - prevPhi) * partialTick;
    }

    // ---- saving and sync ----

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        writeTanks(tag, registries);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        readTanks(tag, registries);
    }

    private void writeTanks(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<String, FluidTank> tank : tanks.entrySet()) {
            tag.put(tank.getKey(), tank.getValue().writeToNBT(registries, new CompoundTag()));
        }
    }

    private void readTanks(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<String, FluidTank> tank : tanks.entrySet()) {
            if (tag.contains(tank.getKey())) {
                tank.getValue().readFromNBT(registries, tag.getCompound(tank.getKey()));
            } else {
                tank.getValue().setFluid(FluidStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (items.getSlots() > 0) {
            tag.put("Items", items.serializeNBT(registries));
        }
        writeTanks(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            // load into a scratch handler so a save with a different slot count can't resize ours
            ItemStackHandler loaded = new ItemStackHandler();
            loaded.deserializeNBT(registries, tag.getCompound("Items"));
            for (int i = 0; i < items.getSlots(); i++) {
                items.setStackInSlot(i, i < loaded.getSlots() ? loaded.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        readTanks(tag, registries);
    }

    // ---- GUI ----

    /** Whether right-clicking opens a GUI. */
    public boolean hasMenu() {
        return false;
    }

    /** Number of int values (progress timers and the like) shown in the GUI, besides the tanks. */
    protected int guiValueCount() {
        return 0;
    }

    /** GUI value {@code index}. */
    protected int getGuiValue(int index) {
        return 0;
    }

    /** Tank fluid ids and amounts first, then the machine's own values. */
    private int rawGuiValue(int index) {
        int tankValues = tanks.size() * 2;
        if (index < tankValues) {
            FluidTank tank = tanks().get(index / 2);
            if (index % 2 == 0) {
                return tank.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
            }
            return tank.getFluidAmount();
        }
        return getGuiValue(index - tankValues);
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    /** A button press from the machine's GUI (AbstractContainerMenu.clickMenuButton); true if handled. Runs on the server. */
    public boolean onMenuButton(Player player, int id) {
        return false;
    }

    /** Adds this machine's slots to its menu at the original GUI positions. */
    public void addMenuSlots(ReactorMenu menu) {
    }

    /** Where the player inventory starts in the GUI (the original's addPlayerInventoryWithOffset). */
    public int inventoryY() {
        return 84;
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ReactorMenu(ReactorMenus.MACHINE.get(), containerId, inventory, this);
    }
}
