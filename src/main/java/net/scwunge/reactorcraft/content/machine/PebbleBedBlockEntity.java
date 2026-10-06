package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.item.TrisoPelletItem;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.HeatConduction;
import net.scwunge.reactorcraft.core.ReactorPart;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.TemperaturedReactorTyped;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorItems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Pebble Bed Reactor Core (TileEntityPebbleBed): a high-temperature gas reactor made of blocks that each hold a pyramid of 47 TRISO fuel pellets.
 * Pellets are fed in at the top and fall to the bottom; every tick a core with fuel may run a decay cycle (more likely the bigger the cluster of
 * pebble beds it is part of, within three blocks), heating itself 20 degrees and now and then using up a stage of a pellet. It cools into the air and
 * the beds beside it, and cannot be cooled by liquid: a carbon dioxide heat exchanger takes its heat. Over 1200 C it wears, and it
 * melts into lava at 4400 C or after a hundred hits of wear.
 */
public class PebbleBedBlockEntity extends FeedingWasteBlockEntity implements TemperaturedReactorTyped, ReactorPart, HeatConduction {
    public static final int SLOTS = 47;
    public static final int MIN_TEMPERATURE = 800;
    public static final int OVER_TEMPERATURE = 1200;
    public static final int FAIL_TEMPERATURE = 4400;

    private int damage;
    private int cycleCooldown;
    private long age;
    private int clusterSize = 1;
    private long clusterTime = -1000;

    public PebbleBedBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.PEBBLE_BED.get(), pos, state, SLOTS);
    }

    @Override
    protected void tickServer() {
        age++;
        if (isFissile() && Chance.of(level.random, fissionChance() / 100D)) {
            runDecayCycle();
        }
        feed();
        if (thermalStep()) {
            updateTemperature();
        }
        if (damage > 0 && level.random.nextInt(800) == 0) {
            damage--;
        }
        if (cycleCooldown > 0) {
            cycleCooldown--;
        }
    }

    /** The chance in percent a tick that a core with fuel runs a cycle, by the size of its cluster. */
    private double fissionChance() {
        int size = clusterSize();
        if (size >= 128) {
            return 1;
        } else if (size >= 72) {
            return 2;
        } else if (size >= 48) {
            return 3;
        } else if (size >= 36) {
            return 4;
        } else if (size >= 24) {
            return 6;
        } else if (size >= 12) {
            return 4;
        } else if (size >= 6) {
            return 2;
        }
        return 1;
    }

    /** How many pebble beds are in this one's cluster: every bed within three blocks of one in it. Looked up again every few seconds. */
    public int clusterSize() {
        if (clusterTime < 0 || level.getGameTime() - clusterTime > 40) {
            clusterTime = level.getGameTime();
            Set<BlockPos> seen = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            seen.add(worldPosition);
            queue.add(worldPosition);
            while (!queue.isEmpty() && seen.size() < 600) {
                BlockPos at = queue.poll();
                for (BlockPos near : BlockPos.betweenClosed(at.offset(-3, -3, -3), at.offset(3, 3, 3))) {
                    if (!seen.contains(near) && level.getBlockEntity(near) instanceof PebbleBedBlockEntity) {
                        BlockPos fixed = near.immutable();
                        seen.add(fixed);
                        queue.add(fixed);
                    }
                }
            }
            clusterSize = seen.size();
        }
        return clusterSize;
    }

    public boolean isFissile() {
        for (int i = 0; i < SLOTS; i++) {
            if (stack(i).is(ReactorItems.TRISO_PELLET.get())) {
                return true;
            }
        }
        return false;
    }

    /** runDecayCycle: the pellet nearest the bottom may wear a stage (3%); the bed heats 20 degrees either way. */
    private void runDecayCycle() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            ItemStack pellet = stack(i);
            if (pellet.is(ReactorItems.TRISO_PELLET.get())) {
                if (Chance.of(level.random, 3)) {
                    setStack(i, fissionProduct(pellet));
                }
                temperature += 20;
                return;
            }
        }
    }

    private static ItemStack fissionProduct(ItemStack pellet) {
        if (pellet.getDamageValue() >= TrisoPelletItem.STAGES - 1) {
            return new ItemStack(ReactorItems.OLD_TRISO_PELLET.get());
        }
        ItemStack next = pellet.copy();
        next.setDamageValue(pellet.getDamageValue() + 1);
        return next;
    }

    @Override
    protected void updateTemperature() {
        super.updateTemperature();
        int ambient = Thermal.ambient(level, worldPosition);
        int dT = temperature - ambient;
        if (dT != 0) {
            int f = Thermal.isExposedToAir(level, worldPosition) ? 24 : 96;
            temperature -= 1 + dT / f;
        }
        if (dT > 0) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
                if (other instanceof PebbleBedBlockEntity bed) {
                    int dTemp = temperature - bed.temperature;
                    if (dTemp > 0) {
                        temperature -= dTemp / 16;
                        bed.temperature += dTemp / 16;
                    }
                }
            }
        }
        if (temperature >= FAIL_TEMPERATURE) {
            toLava();
        } else if (temperature >= OVER_TEMPERATURE) {
            int chance = 5 + (FAIL_TEMPERATURE - temperature) / 10 / Math.max(1, clusterSize());
            if (level.random.nextInt(Math.max(1, chance)) == 0) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 0.5F);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            9, 0.5, 0.5, 0.5, 0);
                }
                damage++;
                if (damage >= 100) {
                    toLava();
                }
            }
        }
        setChanged();
    }

    private void toLava() {
        if (ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner())) {
            level.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
            level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1F, 0.2F);
        } else {
            temperature = FAIL_TEMPERATURE - 1;
            damage = 99;
        }
    }

    // ---- pellets ----

    @Override
    protected int slotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(ReactorItems.TRISO_PELLET.get()) || stack.is(ReactorItems.OLD_TRISO_PELLET.get());
    }

    @Override
    public boolean canInsertFromSide(int slot, ItemStack stack, @Nullable Direction side) {
        return (side == null || side == Direction.UP) && isItemValid(slot, stack);
    }

    /** Spent pellets can always be taken; fresh ones only drip out of the first slot, one every half second during part of each four seconds. */
    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        if (side != null && side != Direction.DOWN) {
            return false;
        }
        if (stack(slot).is(ReactorItems.OLD_TRISO_PELLET.get())) {
            return true;
        }
        if (slot == 0 && cycleCooldown == 0 && age % 80 < 40) {
            cycleCooldown = 10;
            return true;
        }
        return false;
    }

    @Override
    protected void collapseInventory() {
        for (int k = SLOTS - 1; k > 0; k--) {
            if (stack(k).isEmpty() && !stack(k - 1).isEmpty()) {
                setStack(k, stack(k - 1));
                setStack(k - 1, ItemStack.EMPTY);
                return;
            }
        }
    }

    // ---- heat ----

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    @Override
    public int getMaxTemperature() {
        return FAIL_TEMPERATURE;
    }

    @Override
    public boolean canDumpHeatInto(CoolantState coolant) {
        return false;
    }

    @Override
    public ReactorType getReactorType() {
        return ReactorType.HTGR;
    }

    @Override
    public boolean allowExternalHeating() {
        return false;
    }

    @Override
    public boolean allowHeatExtraction() {
        return true;
    }

    public int damage() {
        return damage;
    }

    // ---- GUI: a pyramid of slots (11, 11, 9, 7, 5, 3, 1) ----

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public void addMenuSlots(ReactorMenu menu) {
        int[] rows = {11, 11, 9, 7, 5, 3, 1};
        int id = 0;
        for (int f = 0; f < rows.length; f++) {
            int dx = 18 * (11 - rows[f]) / 2;
            for (int i = 0; i < rows[f]; i++) {
                menu.addMachineSlot(id++, 21 + 18 * i + dx, 25 + 18 * f);
            }
        }
    }

    @Override
    public int inventoryX() {
        return 39;
    }

    @Override
    public int inventoryY() {
        return 161;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Damage", damage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        damage = tag.getInt("Damage");
    }
}
