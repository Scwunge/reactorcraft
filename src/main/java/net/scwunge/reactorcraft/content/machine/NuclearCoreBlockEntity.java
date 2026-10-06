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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.Linkable;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.RadiationHooks;
import net.scwunge.reactorcraft.core.ReactorPart;
import net.scwunge.reactorcraft.core.ReactorMeltdownEvent;
import net.scwunge.reactorcraft.core.TemperaturedReactorTyped;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorTickets;
import org.jetbrains.annotations.Nullable;

/**
 * The shared part of every fuel-holding reactor core (TileEntityNuclearCore): twelve slots (four of fuel that feed down a
 * column of cores, eight of spent fuel and waste), decay neutrons from fuel, a "tickle" that keeps the chunks around loaded
 * while neutrons keep hitting it, heat that passes into neighbouring cores, hydrogen from overheated cladding, and meltdown.
 */
public abstract class NuclearCoreBlockEntity extends ReactorMachineBlockEntity implements TemperaturedReactorTyped, Linkable, Feedable,
        NeutronTile, ReactorPart {
    /** Fuel in slots 0 to 3 (3 is the one reacting); spent fuel and waste in 4 to 11. */
    public static final int FUEL_SLOTS = 4;
    public static final int SLOTS = 12;
    public static final int CLADDING = 800;
    public static final int HYDROGEN = 1400;
    public static final int MELTDOWN = 1800;
    private static final int MAX_HYDROGEN = 200;
    /** Two minutes without a neutron and the core stops keeping its chunks loaded. */
    private static final int ACTIVE_TICKS = 2400;

    protected int hydrogen;
    private int activeTimer;
    @Nullable
    private BlockPos cpu;

    protected NuclearCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOTS);
    }

    public abstract boolean isFissile();

    @Override
    public final void link(BlockPos cpu) {
        this.cpu = cpu;
        setChanged();
    }

    @Nullable
    @Override
    public final BlockPos cpu() {
        return cpu;
    }

    @Override
    protected void tickServer() {
        if (isFissile() && random().nextInt(decayNeutronChance()) == 0) {
            level.addFreshEntity(new NeutronEntity(level, worldPosition, randomDirection(false), NeutronType.DECAY));
        }
        feedWaste();
        feed();
        if (activeTimer > 0) {
            activeTimer--;
            if (activeTimer == 0) {
                onActivityChange(false);
            }
        }
        if (thermalStep()) {
            updateTemperature();
        }
        if (temperature > CLADDING) {
            if (random().nextInt(20) == 0) {
                fizz();
            }
            smoke(9);
        } else if (temperature > warningTemperature() && random().nextInt(5) == 0) {
            if (random().nextInt(20) == 0) {
                fizz();
            }
            smoke(4);
        }
    }

    private net.minecraft.util.RandomSource random() {
        return level.random;
    }

    protected int decayNeutronChance() {
        return 20;
    }

    protected int warningTemperature() {
        return 500;
    }

    /** getRandomDirection: sideways, or any way if allowed. */
    protected final Direction randomDirection(boolean allowVertical) {
        Direction[] all = Direction.values();
        return allowVertical ? all[random().nextInt(6)] : all[2 + random().nextInt(4)];
    }

    private void fizz() {
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
    }

    private void smoke(int count) {
        if (level instanceof ServerLevel server) {
            for (int i = 0; i < count; i++) {
                server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + random().nextDouble(), worldPosition.getY() + random().nextDouble(),
                        worldPosition.getZ() + random().nextDouble(), 1, 0, 0, 0, 0);
            }
        }
    }

    // ---- activity and chunk loading ----

    public final boolean isActive() {
        return activeTimer > 0;
    }

    private void onActivityChange(boolean active) {
        if (level instanceof ServerLevel server && ReactorConfig.CHUNKLOADING.get()) {
            ChunkPos chunk = new ChunkPos(worldPosition);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ReactorTickets.CONTROLLER.forceChunk(server, worldPosition, chunk.x + dx, chunk.z + dz, active, true);
                }
            }
        }
        syncToClient();
    }

    @Override
    public void setRemoved() {
        if (cpu != null && level != null && level.getBlockEntity(cpu) instanceof CpuBlockEntity c) {
            c.removeTemperatureCheck(worldPosition);
        }
        if (activeTimer > 0 && level instanceof ServerLevel) {
            activeTimer = 0;
            onActivityChange(false);
        }
        super.setRemoved();
    }

    /** onNeutron: any neutron wakes the core up; it is only absorbed by what the subclass decides. */
    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        boolean inactive = activeTimer <= 0;
        activeTimer = ACTIVE_TICKS;
        if (inactive) {
            onActivityChange(true);
        }
        return false;
    }

    // ---- feeding: fuel moves down a column of cores, spent fuel and waste moves down with it ----

    private void feedWaste() {
        if (level.getBlockEntity(worldPosition.below()) instanceof NuclearCoreBlockEntity below) {
            for (int i = FUEL_SLOTS; i < SLOTS; i++) {
                if (stack(i).isEmpty()) {
                    continue;
                }
                for (int k = FUEL_SLOTS; k < SLOTS; k++) {
                    if (below.stack(k).isEmpty()) {
                        below.setStack(k, stack(i));
                        setStack(i, ItemStack.EMPTY);
                        break;
                    }
                }
            }
        }
    }

    private void feed() {
        if (level.getBlockEntity(worldPosition.below()) instanceof Feedable below && below.feedIn(stack(3))) {
            setStack(3, stack(2));
            setStack(2, stack(1));
            setStack(1, stack(0));
            setStack(0, level.getBlockEntity(worldPosition.above()) instanceof Feedable above ? above.feedOut() : ItemStack.EMPTY);
        }
        collapseFuel();
    }

    private void collapseFuel() {
        for (int k = 3; k > 0; k--) {
            if (stack(k).isEmpty() && !stack(k - 1).isEmpty()) {
                setStack(k, stack(k - 1));
                setStack(k - 1, ItemStack.EMPTY);
                return;
            }
        }
    }

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
        ItemStack last = stack(3);
        if (last.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setStack(3, ItemStack.EMPTY);
        return last.copy();
    }

    /** tryPushSpentFuel: moves what is in a fuel slot to the first free spent-fuel slot. */
    protected final void pushSpentFuel(int slot) {
        for (int i = FUEL_SLOTS; i < SLOTS; i++) {
            if (stack(i).isEmpty()) {
                setStack(i, stack(slot));
                setStack(slot, ItemStack.EMPTY);
                return;
            }
        }
    }

    /** checkPoisonedChance: the more waste has built up, the likelier a neutron is soaked up without causing fission. */
    protected final boolean isPoisoned() {
        int count = 0;
        for (int i = FUEL_SLOTS; i < SLOTS; i++) {
            if (WasteManager.isWaste(stack(i))) {
                count++;
            }
        }
        return random().nextInt(9 - count) == 0;
    }

    /** addWaste: a piece of fission waste goes into spent fuel, on top of the same waste if there is some. */
    protected final void addWaste() {
        ItemStack waste = WasteManager.randomWasteItem(random());
        for (int i = FUEL_SLOTS; i < SLOTS; i++) {
            ItemStack inSlot = stack(i);
            if (inSlot.isEmpty()) {
                setStack(i, waste);
                return;
            }
            if (ItemStack.isSameItemSameComponents(waste, inSlot) && inSlot.getCount() + 1 <= waste.getMaxStackSize()) {
                ItemStack grown = inSlot.copy();
                grown.grow(1);
                setStack(i, grown);
                return;
            }
        }
    }

    /** spawnNeutronBurst: a fission throws out three neutrons. */
    protected final void spawnNeutronBurst() {
        NeutronType type = reactorTypeOrNull() != null ? reactorTypeOrNull().neutronType() : null;
        if (type == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            level.addFreshEntity(new NeutronEntity(level, worldPosition, randomDirection(ReactorConfig.VERTICAL_NEUTRONS.get()), type));
        }
    }

    @Nullable
    private net.scwunge.reactorcraft.core.ReactorType reactorTypeOrNull() {
        return getReactorType();
    }

    // ---- heat, hydrogen and meltdown ----

    @Override
    public final int getTemperature() {
        return temperature;
    }

    @Override
    public final void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    @Override
    public int getMaxTemperature() {
        return MELTDOWN;
    }

    protected int restingTemperature() {
        return Thermal.ambient(level, worldPosition);
    }

    protected int ambientHeatLossFactor(int base, int ambient) {
        return base;
    }

    /** The reactor part heat exchange, then a core's own: loss to the surroundings, sharing with the cores beside it, hydrogen, meltdown. */
    @Override
    protected void updateTemperature() {
        super.updateTemperature();
        int resting = restingTemperature();
        int dT = temperature - resting;
        if (dT != 0) {
            int d = Thermal.isExposedToAir(level, worldPosition) ? 32 : 64;
            d = ambientHeatLossFactor(d, resting);
            temperature -= 1 + dT / d;
        }
        if (dT > 0) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockEntity other = level.getBlockEntity(worldPosition.relative(dir));
                if (other != null && other.getType() == getType()) {
                    NuclearCoreBlockEntity core = (NuclearCoreBlockEntity) other;
                    int dTemp = temperature - core.temperature;
                    if (dTemp > 0) {
                        int d = 16;
                        temperature -= dTemp / d;
                        core.temperature += (int) (dTemp / d * heatConductionEfficiency(core));
                    }
                }
            }
        }
        if (temperature > MELTDOWN) {
            onMeltdown();
        }
        if (temperature > HYDROGEN) {
            hydrogen++;
            if (hydrogen > MAX_HYDROGEN) {
                hydrogenExplosion();
            }
        } else if (hydrogen > 0) {
            hydrogen--;
        }
        setChanged();
    }

    /** onMeltdown: nearby cores of this kind turn to corium and the core blows up, contaminating the area. */
    protected void onMeltdown() {
        NeoForge.EVENT_BUS.post(new ReactorMeltdownEvent(level, worldPosition));
        boolean destroys = ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.griefingAllowed(level);
        if (!destroys) {
            // no destruction allowed: the core just stays at its limit, and the area is still contaminated
            temperature = MELTDOWN;
            RadiationHooks.contaminateReactor(level, worldPosition);
            return;
        }
        int r = 2;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-r, -r, -r), worldPosition.offset(r, r, r))) {
            BlockEntity other = level.getBlockEntity(pos);
            if (other != null && other.getType() == getType() && WorldSafety.mayChange(level, pos, owner())) {
                level.setBlockAndUpdate(pos, ReactorBlocks.CORIUM_BLOCK.get().defaultBlockState());
            }
        }
        if (WorldSafety.mayChange(level, worldPosition, owner())) {
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 8F, false,
                    Level.ExplosionInteraction.BLOCK);
        }
        RadiationHooks.contaminateReactor(level, worldPosition);
        hydrogenExplosion();
    }

    protected final void hydrogenExplosion() {
        hydrogen = 0;
        HydrogenExplosion.detonate(level, worldPosition, 7F, owner());
    }

    // ---- items ----

    @Override
    protected int slotLimit(int slot) {
        return 1;
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
        return (side == null || side == Direction.DOWN) && canRemove(stack(slot));
    }

    /** What may be taken out by automation. */
    protected abstract boolean canRemove(ItemStack stack);

    @Override
    public boolean canDumpHeatInto(CoolantState coolant) {
        return coolant.isWater();
    }

    // ---- GUI ----

    @Override
    public boolean hasMenu() {
        return true;
    }

    private static final int[][] SLOT_POSITIONS = {
            {80, 23}, {80, 41}, {80, 59}, {80, 77},
            {53, 23}, {53, 41}, {53, 59}, {53, 77},
            {107, 23}, {107, 41}, {107, 59}, {107, 77}};

    @Override
    public void addMenuSlots(ReactorMenu menu) {
        for (int i = 0; i < SLOTS; i++) {
            if (i < FUEL_SLOTS) {
                menu.addMachineSlot(i, SLOT_POSITIONS[i][0], SLOT_POSITIONS[i][1]);
            } else {
                menu.addOutputSlot(i, SLOT_POSITIONS[i][0], SLOT_POSITIONS[i][1]);
            }
        }
    }

    @Override
    public int inventoryY() {
        return 100;
    }

    @Override
    protected int guiValueCount() {
        return 1;
    }

    @Override
    protected int getGuiValue(int index) {
        return temperature;
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ActiveTimer", activeTimer);
        tag.putInt("Hydrogen", hydrogen);
        if (cpu != null) {
            tag.putLong("Cpu", cpu.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        activeTimer = tag.getInt("ActiveTimer");
        hydrogen = tag.getInt("Hydrogen");
        cpu = tag.contains("Cpu") ? BlockPos.of(tag.getLong("Cpu")) : null;
    }
}
