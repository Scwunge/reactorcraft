package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.waste.Isotope;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.NeutronType;
import net.minecraft.world.level.Level;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Waste Decayer (TileEntityWasteDecayer): a reactor part that turns long-lived waste into what it decays into, when
 * neutrons hit it. The hotter it is the better it works, from 150 C (5%) to 400 C (25%). Stack them in a column and
 * waste moves down through them; waste goes in at the top and comes out of the bottom, and only the waste that is
 * no longer long-lived can be taken out.
 */
public class WasteDecayerBlockEntity extends FeedingWasteBlockEntity implements NeutronTile {
    public static final int SLOTS = 15;
    public static final int BASE_TEMPERATURE = 150;
    public static final int OPTIMAL_TEMPERATURE = 400;

    private static final TagKey<Item> LEAD = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.parse("c:ingots/lead"));

    public WasteDecayerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.WASTE_DECAYER.get(), pos, state, SLOTS);
    }

    @Override
    protected void tickServer() {
        feed();
        if (thermalStep()) {
            updateTemperature();
        }
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        return onNeutron(neutron.neutronType(), neutron.neutronSpeed());
    }

    /**
     * onNeutron: a fission-type neutron hitting the decayer is absorbed half the time, and then may decay one long-lived
     * waste item. Returns whether the neutron was absorbed.
     */
    public boolean onNeutron(NeutronType type, NeutronType.NeutronSpeed speed) {
        if (!level.isClientSide && type.canIrradiateMaterials() && Chance.of(level.random, 50)) {
            if (Chance.of(level.random, decayChance() * speed.wasteConversionMultiplier())) {
                tryDecay();
            }
            return true;
        }
        return false;
    }

    public int countWaste() {
        int count = 0;
        for (int i = 0; i < size(); i++) {
            if (WasteManager.isWaste(stack(i))) {
                count += stack(i).getCount();
            }
        }
        return count;
    }

    /** Percent chance per absorbed neutron, from the temperature. */
    public double decayChance() {
        double t = temperature;
        return t < BASE_TEMPERATURE ? interpolate(t, 20, BASE_TEMPERATURE, 0, 5) : interpolate(t, BASE_TEMPERATURE, OPTIMAL_TEMPERATURE, 5, 25);
    }

    private static double interpolate(double x, double x1, double x2, double y1, double y2) {
        return y1 + (x - x1) / (x2 - x1) * (y2 - y1);
    }

    private boolean tryDecay() {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = stack(i);
            if (WasteManager.isLongLived(stack) && tryDecay(i, stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryDecay(int slot, ItemStack stack) {
        Isotope.DecayData split = WasteManager.isotope(stack).decay();
        if (split == null) {
            shrink(slot, 1);
            return true;
        }
        int amount = (int) Math.floor(split.amount());
        if (Chance.of(level.random, split.amount() - amount)) {
            amount++;
        }
        if (amount == 0) {
            return false;
        }
        ItemStack product = productStack(split, amount);
        if (product.isEmpty()) {
            return true;
        }
        if (addToInventory(product)) {
            shrink(slot, 1);
            return true;
        }
        return false;
    }

    /** Waste for an isotope product; lead (if some mod has it) for lead; nothing otherwise. */
    private ItemStack productStack(Isotope.DecayData split, int amount) {
        Isotope isotope = split.product().isotope();
        if (isotope != null) {
            return WasteManager.waste(isotope, amount);
        }
        return BuiltInRegistries.ITEM.getTag(LEAD).flatMap(tag -> tag.stream().findFirst())
                .map(holder -> new ItemStack(holder.value(), amount)).orElse(ItemStack.EMPTY);
    }

    /** ReikaInventoryHelper.addToIInv: puts the whole stack in, or nothing if it will not fit. */
    private boolean addToInventory(ItemStack product) {
        int room = 0;
        for (int i = 0; i < size(); i++) {
            ItemStack existing = stack(i);
            int limit = Math.min(slotLimit(i), product.getMaxStackSize());
            if (existing.isEmpty()) {
                room += limit;
            } else if (ItemStack.isSameItemSameComponents(existing, product)) {
                room += limit - existing.getCount();
            }
        }
        if (room < product.getCount()) {
            return false;
        }
        ItemStack remaining = product.copy();
        for (int i = 0; i < size() && !remaining.isEmpty(); i++) {
            remaining = items().insertItem(i, remaining, false);
        }
        return true;
    }

    /** Waste moves one step down per tick, and equal stacks merge a little at a time. */
    @Override
    protected void collapseInventory() {
        for (int k = size() - 1; k > 0; k--) {
            ItemStack from = stack(k - 1);
            ItemStack to = stack(k);
            if (to.isEmpty() && !from.isEmpty()) {
                setStack(k, from);
                setStack(k - 1, ItemStack.EMPTY);
                return;
            }
            if (!from.isEmpty() && ItemStack.isSameItemSameComponents(to, from)
                    && to.getCount() < Math.min(to.getMaxStackSize(), slotLimit(k))) {
                ItemStack merged = to.copy();
                merged.grow(1);
                setStack(k, merged);
                shrink(k - 1, 1);
                return;
            }
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return WasteManager.isWaste(stack);
    }

    @Override
    protected int slotLimit(int slot) {
        return 8;
    }

    @Override
    public int[] slotsForFace(@Nullable Direction side) {
        return side == Direction.UP || side == Direction.DOWN || side == null ? super.slotsForFace(side) : new int[0];
    }

    @Override
    public boolean canInsertFromSide(int slot, ItemStack stack, @Nullable Direction side) {
        return (side == null || side == Direction.UP) && isItemValid(slot, stack);
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return (side == null || side == Direction.DOWN) && !WasteManager.isLongLived(stack(slot));
    }

    // ---- GUI ----

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public void addMenuSlots(ReactorMenu menu) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                menu.addMachineSlot(row * 5 + col, 44 + col * 18, 22 + row * 18);
            }
        }
    }

    @Override
    public int inventoryY() {
        return 93;
    }

    @Override
    protected int guiValueCount() {
        return 1;
    }

    @Override
    protected int getGuiValue(int index) {
        return temperature;
    }
}
