package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The control rods a Central Control block runs (the original's ReactorControlLayout): their positions, the box around them
 * relative to the CPU, and what the GUI shows for each. Rods are looked up in the world, so the layout only keeps positions.
 */
public final class ControlLayout {
    /** What a CPU needs per control rod to hold them up: 1024 W each. */
    public static final long POWER_PER_ROD = 1024;

    private final BlockPos controller;
    private final Set<BlockPos> rods = new LinkedHashSet<>();
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    public ControlLayout(BlockPos controller) {
        this.controller = controller;
    }

    public void clear() {
        rods.clear();
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
    }

    public void add(BlockPos pos) {
        if (rods.add(pos.immutable())) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }
    }

    public void remove(BlockPos pos) {
        if (rods.remove(pos)) {
            List<BlockPos> left = new ArrayList<>(rods);
            clear();
            left.forEach(this::add);
        }
    }

    public boolean isEmpty() {
        return rods.isEmpty();
    }

    public int count() {
        return rods.size();
    }

    public Collection<BlockPos> positions() {
        return Collections.unmodifiableSet(rods);
    }

    public boolean contains(BlockPos pos) {
        return rods.contains(pos);
    }

    public long minPower() {
        return POWER_PER_ROD * rods.size();
    }

    // limits relative to the controller; with no rods they are 0
    public int minX() {
        return rods.isEmpty() ? 0 : minX - controller.getX();
    }

    public int maxX() {
        return rods.isEmpty() ? 0 : maxX - controller.getX();
    }

    public int minY() {
        return rods.isEmpty() ? 0 : minY - controller.getY();
    }

    public int maxY() {
        return rods.isEmpty() ? 0 : maxY - controller.getY();
    }

    public int minZ() {
        return rods.isEmpty() ? 0 : minZ - controller.getZ();
    }

    public int maxZ() {
        return rods.isEmpty() ? 0 : maxZ - controller.getZ();
    }

    /** The rod {@code dx, dy, dz} away from the controller, or null. */
    public ControlRodBlockEntity rodAt(Level level, int dx, int dy, int dz) {
        BlockPos pos = controller.offset(dx, dy, dz);
        if (!rods.contains(pos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof ControlRodBlockEntity rod ? rod : null;
    }

    public int countLowered(Level level) {
        int count = 0;
        for (BlockPos pos : rods) {
            if (level.getBlockEntity(pos) instanceof ControlRodBlockEntity rod && rod.isActive()) {
                count++;
            }
        }
        return count;
    }

    /** SCRAM: every rod drops, and only the first makes a noise. */
    public void scram(Level level) {
        boolean first = true;
        for (BlockPos pos : rods) {
            if (level.getBlockEntity(pos) instanceof ControlRodBlockEntity rod) {
                rod.drop(first);
                first = false;
            }
        }
    }

    public void forEachRod(Level level, java.util.function.Consumer<ControlRodBlockEntity> action) {
        for (BlockPos pos : rods) {
            if (level.getBlockEntity(pos) instanceof ControlRodBlockEntity rod) {
                action.accept(rod);
            }
        }
    }

    public long[] toLongs() {
        return rods.stream().mapToLong(BlockPos::asLong).toArray();
    }

    public void load(long[] positions) {
        clear();
        for (long l : positions) {
            add(BlockPos.of(l));
        }
    }
}
