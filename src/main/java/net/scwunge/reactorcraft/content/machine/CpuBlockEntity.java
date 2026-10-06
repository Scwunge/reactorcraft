package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.content.item.RemoteControlItem;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.Linkable;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.ReactorPart;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.ScramEvent;
import net.scwunge.reactorcraft.core.TemperaturedReactorTyped;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Central Control (TileEntityCPU): finds every control rod in the reactor it is part of (within 12 blocks sideways and 4 up
 * and down, across connected reactor parts) and runs them off RotaryCraft shaft power, 1024 W per rod. If the power fails, or
 * it or a core linked to it with the remote gets too hot, it SCRAMs: all rods drop. Its GUI shows the rods from above and
 * toggles them; a comparator reads how many are lowered.
 */
public class CpuBlockEntity extends ReactorMachineBlockEntity implements TemperaturedReactorTyped, ReactorPart, NeutronTile {
    public static final int MAX_TEMPERATURE = 800;
    private static final int RADIUS = 12;
    private static final int HEIGHT = 4;
    /** Button ids from the GUI: raise all, lower all, and 1000 + a rod position for toggling one. */
    public static final int BUTTON_RAISE_ALL = 0;
    public static final int BUTTON_LOWER_ALL = 1;
    public static final int BUTTON_TOGGLE_BASE = 1000;

    private final ShaftInput shaft = new ShaftInput();
    private final ControlLayout layout;
    private final List<BlockPos> monitors = new ArrayList<>();
    private int redstoneUpdate = 200;
    private long age;

    public CpuBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.CPU.get(), pos, state, 0);
        layout = new ControlLayout(pos);
    }

    public ControlLayout layout() {
        return layout;
    }

    public ShaftInput shaft() {
        return shaft;
    }

    @Override
    protected void tickServer() {
        age++;
        if (thermalStep()) {
            updateTemperature();
        }
        if (level.getGameTime() % 64 == 0) {
            layout.clear();
        }
        if (layout.isEmpty()) {
            findRods();
            markForSync();
        }
        long before = shaft.power();
        shaft.read(level, worldPosition, Direction.values());
        if (before != shaft.power()) {
            markForSync();
        }
        long min = layout.minPower();
        if (shaft.power() < min && age > 20) {
            scram();
        }
        if (layout.count() > 0 && shaft.power() >= min * 4 && (temperature > MAX_TEMPERATURE || hottestMonitor() > MAX_TEMPERATURE)) {
            scram();
        }
        if (redstoneUpdate > 0) {
            redstoneUpdate--;
            if (redstoneUpdate <= 0) {
                syncToClient();
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
        }
    }

    /** Walks across connected reactor parts from this block and takes note of the control rods. */
    private void findRods() {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        seen.add(worldPosition);
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            BlockEntity be = level.getBlockEntity(at);
            if (be instanceof ControlRodBlockEntity) {
                layout.add(at);
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = at.relative(dir);
                if (!seen.add(next) || Math.abs(next.getX() - worldPosition.getX()) > RADIUS || Math.abs(next.getZ() - worldPosition.getZ()) > RADIUS
                        || Math.abs(next.getY() - worldPosition.getY()) > HEIGHT) {
                    continue;
                }
                if (level.getBlockEntity(next) instanceof ReactorPart) {
                    queue.add(next);
                }
            }
        }
    }

    private int hottestMonitor() {
        if (monitors.isEmpty()) {
            return 0;
        }
        BlockPos pick = monitors.get(level.random.nextInt(monitors.size()));
        return level.getBlockEntity(pick) instanceof TemperaturedReactorTyped core ? core.getTemperature() : 0;
    }

    /** SCRAM: drops every rod. */
    public void scram() {
        NeoForge.EVENT_BUS.post(new ScramEvent(level, worldPosition, temperature));
        layout.scram(level);
        if (redstoneUpdate == 0) {
            redstoneUpdate = 7;
        }
    }

    public void lowerAllRods() {
        layout.forEachRod(level, rod -> rod.setActive(true, false));
        click();
        redstoneUpdate = 30;
    }

    public void raiseAllRods() {
        layout.forEachRod(level, rod -> rod.setActive(false, false));
        click();
        redstoneUpdate = 30;
    }

    private void click() {
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1F, 1.3F);
    }

    /** The GUI's buttons: raise all, lower all, or toggle one rod (id 1000 + the rod's position packed relative to the CPU). */
    @Override
    public boolean onMenuButton(Player player, int id) {
        if (id == BUTTON_RAISE_ALL) {
            raiseAllRods();
            return true;
        }
        if (id == BUTTON_LOWER_ALL) {
            lowerAllRods();
            return true;
        }
        if (id >= BUTTON_TOGGLE_BASE) {
            int packed = id - BUTTON_TOGGLE_BASE;
            BlockPos at = worldPosition.offset((packed & 63) - 32, (packed >> 6 & 63) - 32, (packed >> 12 & 63) - 32);
            if (layout.contains(at) && level.getBlockEntity(at) instanceof ControlRodBlockEntity rod) {
                rod.toggle(true, false);
                redstoneUpdate = 30;
                return true;
            }
        }
        return false;
    }

    /** What the GUI sends to toggle the rod at {@code dx, dy, dz} from the CPU. */
    public static int toggleButton(int dx, int dy, int dz) {
        return BUTTON_TOGGLE_BASE + ((dx + 32) | (dy + 32) << 6 | (dz + 32) << 12);
    }

    /** A comparator reads 15 times the fraction of rods that are lowered. */
    public int redstoneOutput() {
        return layout.isEmpty() ? 0 : 15 * layout.countLowered(level) / layout.count();
    }

    // ---- linking cores for temperature checks (the remote control does this) ----

    public void addTemperatureCheck(Linkable core, BlockPos corePos) {
        if (!monitors.contains(corePos)) {
            monitors.add(corePos);
        }
        core.link(worldPosition);
        setChanged();
    }

    public void removeTemperatureCheck(BlockPos corePos) {
        monitors.remove(corePos);
        setChanged();
    }

    // ---- reactor part ----

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        return false;
    }

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
        return MAX_TEMPERATURE;
    }

    @Override
    public boolean canDumpHeatInto(CoolantState coolant) {
        return coolant != CoolantState.EMPTY;
    }

    @Override
    public ReactorType getReactorType() {
        return ReactorType.FISSION;
    }

    // ---- GUI ----

    /** The screen also stays open for a player holding a remote control linked to this CPU. */
    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) || level != null && level.getBlockEntity(worldPosition) == this
                && (RemoteControlItem.canReach(player.getMainHandItem(), player, worldPosition)
                || RemoteControlItem.canReach(player.getOffhandItem(), player, worldPosition));
    }

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public int inventoryY() {
        return 128;
    }

    // ---- saving and sync ----

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        shaft.save(tag);
        tag.putLongArray("Rods", layout.toLongs());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        shaft.load(tag);
        layout.load(tag.getLongArray("Rods"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BlockPos pos : monitors) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put("Monitors", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        monitors.clear();
        for (net.minecraft.nbt.Tag t : tag.getList("Monitors", net.minecraft.nbt.Tag.TAG_LONG)) {
            monitors.add(BlockPos.of(((LongTag) t).getAsLong()));
        }
    }
}
