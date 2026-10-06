package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.waste.Isotope;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.RadiationHooks;

/**
 * TileEntityWasteUnit: a block that holds nuclear waste while it decays. Every tick each isotope has a chance to give off
 * radiation (if the unit leaks) and a chance, tied to its half-life, to decay away: half the stack, at least one item.
 */
public abstract class WasteUnitBlockEntity extends FeedingWasteBlockEntity {
    /** The original's decay scale: a unit's decay rate times this multiplies every chance. */
    private static final int DECAY_SCALE = 192;

    private long lastTickTime = -1;

    protected WasteUnitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public abstract boolean leaksRadiation();

    public abstract boolean isValidIsotope(Isotope isotope);

    protected abstract double baseDecayRate();

    /** Whether decay also catches up on real time that passed while the world was closed or lagging. */
    protected abstract boolean accountForOutGameTime();

    private double accelerationFactor() {
        return baseDecayRate() * DECAY_SCALE;
    }

    protected final void decayWaste() {
        double mult = accelerationFactor();
        if (accountForOutGameTime()) {
            mult *= 1 + skippedTicks();
        }
        for (int i = 0; i < size(); i++) {
            ItemStack stack = stack(i);
            Isotope atom = WasteManager.isotope(stack);
            if (atom == null) {
                continue;
            }
            double halfLife = atom.halfLifeTicks();
            double leakChance = mult / baseDecayRate() * 0.5 * Math.log(2) / Math.log(halfLife);
            if (Chance.of(level.random, leakChance)) {
                if (leaksRadiation() && level.random.nextBoolean()) {
                    leakRadiation();
                }
            }
            if (Chance.of(level.random, mult * Math.log(2) / halfLife)) {
                shrink(i, Math.max(1, stack.getCount() / 2));
                onDecayWaste(i);
            }
        }
    }

    /** Compensates for lag and lets decay run while the world is closed, in real time (50 ms to the tick). */
    private long skippedTicks() {
        long now = System.currentTimeMillis();
        long ticks = 0;
        if (lastTickTime >= 0) {
            long dur = now - lastTickTime;
            if (dur > 50) {
                ticks = dur / 50 - 1;
            }
        }
        lastTickTime = now;
        return ticks;
    }

    protected void onDecayWaste(int slot) {
    }

    private void leakRadiation() {
        RadiationHooks.leakNeutron(level, worldPosition, Direction.getRandom(level.random));
    }

    @Override
    public final boolean isItemValid(int slot, ItemStack stack) {
        Isotope isotope = WasteManager.isotope(stack);
        return isotope != null && isValidIsotope(isotope) && isValidSlot(slot, stack);
    }

    protected boolean isValidSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected int slotLimit(int slot) {
        return 1;
    }

    public final int countWaste() {
        int count = 0;
        for (int i = 0; i < size(); i++) {
            if (WasteManager.isWaste(stack(i))) {
                count += stack(i).getCount();
            }
        }
        return count;
    }

    public final boolean hasWaste() {
        return countWaste() > 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LastTime", lastTickTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastTickTime = tag.contains("LastTime") ? tag.getLong("LastTime") : -1;
    }

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    protected void tickClient() {
    }
}
