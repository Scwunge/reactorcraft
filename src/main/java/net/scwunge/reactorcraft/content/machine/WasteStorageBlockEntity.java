package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.waste.Isotope;
import net.scwunge.reactorcraft.core.RadiationHooks;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Waste Storage (TileEntityWasteStorage): twelve slots of up to 16 for the long-lived waste (half-life over six years),
 * which the container will not take. It does not leak, but anything in sight nearby is made sick, more so the more waste
 * is in it. Automation can put waste in but never take it out. In the Nether, or anywhere above 100 C, it smokes and
 * will eventually blow up. It decays waste even while the world is closed, in real time.
 */
public class WasteStorageBlockEntity extends WasteUnitBlockEntity {
    public static final int SLOTS = 12;
    public static final int STACK_LIMIT = 16;

    public WasteStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.WASTE_STORAGE.get(), pos, state, SLOTS);
    }

    @Override
    protected void tickServer() {
        if (level.random.nextInt(20) == 0) {
            RadiationHooks.sickenMobs(level, worldPosition, range());
        }
        decayWaste();
        feed();
        if ((level.dimensionType().ultraWarm() || Thermal.ambient(level, worldPosition) > 100) && hasWaste()) {
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        3, 1.5, 1.5, 1.5, 0);
            }
            if (level.random.nextInt(4) == 0) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
            }
            if (level.random.nextInt(200) == 0 && ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get()
                    && WorldSafety.mayChange(level, worldPosition, owner())) {
                level.removeBlock(worldPosition, false);
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 4F, true,
                        Level.ExplosionInteraction.BLOCK);
            }
        }
    }

    /** The radius it sickens things in: the square root of the waste inside. */
    public int range() {
        return rangeFromWasteCount(countWaste());
    }

    public int maxRange() {
        return rangeFromWasteCount(SLOTS);
    }

    public static int rangeFromWasteCount(int count) {
        return (int) Math.sqrt(count);
    }

    /** isAppropriateWasteSlot: waste goes on top of the same waste if there is room, otherwise into an empty slot. */
    @Override
    protected boolean isValidSlot(int slot, ItemStack stack) {
        for (int i = 0; i < size(); i++) {
            ItemStack existing = stack(i);
            if (ItemStack.isSameItemSameComponents(stack, existing)
                    && existing.getCount() + stack.getCount() <= Math.min(STACK_LIMIT, stack.getMaxStackSize())) {
                return i == slot;
            }
        }
        return stack(slot).isEmpty();
    }

    @Override
    protected void collapseInventory() {
        for (int i = 0; i < size(); i++) {
            for (int k = size() - 1; k > 0; k--) {
                ItemStack from = stack(k - 1);
                if (from.isEmpty()) {
                    continue;
                }
                ItemStack to = stack(k);
                if (to.isEmpty()) {
                    setStack(k, from);
                    setStack(k - 1, ItemStack.EMPTY);
                } else if (ItemStack.isSameItemSameComponents(to, from)
                        && to.getCount() + from.getCount() <= Math.min(STACK_LIMIT, to.getMaxStackSize())) {
                    ItemStack merged = to.copy();
                    merged.grow(from.getCount());
                    setStack(k, merged);
                    setStack(k - 1, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public boolean leaksRadiation() {
        return false;
    }

    @Override
    public boolean isValidIsotope(Isotope isotope) {
        return isotope.isLongLived();
    }

    @Override
    protected double baseDecayRate() {
        return 1.75;
    }

    @Override
    protected boolean accountForOutGameTime() {
        return true;
    }

    @Override
    protected int slotLimit(int slot) {
        return STACK_LIMIT;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return false;
    }

    // ---- GUI ----

    private static final int[][] SLOT_POSITIONS = {
            {70, 20}, {90, 20},
            {50, 40}, {70, 40}, {90, 40}, {110, 40},
            {50, 60}, {70, 60}, {90, 60}, {110, 60},
            {70, 80}, {90, 80}};

    @Override
    public void addMenuSlots(ReactorMenu menu) {
        for (int i = 0; i < SLOTS; i++) {
            menu.addMachineSlot(i, SLOT_POSITIONS[i][0], SLOT_POSITIONS[i][1]);
        }
    }

    @Override
    public int inventoryY() {
        return 104;
    }
}
