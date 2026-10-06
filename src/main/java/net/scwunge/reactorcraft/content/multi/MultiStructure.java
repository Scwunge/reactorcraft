package net.scwunge.reactorcraft.content.multi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A multiblock structure (the original's BlockMultiBlock checks, as data): a list of cells, each saying what block must stand at an offset from the
 * structure's origin. When a block of the structure is placed (or a controller looks), every way the placed block could fill a cell is tried; if the whole
 * structure matches, its {@link MultiPartBlock}s are switched to their formed look and the controllers inside are told. Breaking any block of a formed
 * structure unforms what is round it.
 *
 * <p>Offsets are (x, y, z) from the origin; a rotatable structure may also stand turned by 90, 180 or 270 degrees about the origin.
 */
public class MultiStructure {
    private static final int MAX_FLOOD = 1200;

    /** One required block; {@code sample} is a block that satisfies it (air for an empty cell), for building or showing the structure. */
    public record Cell(int x, int y, int z, Predicate<BlockState> match, boolean controller, Supplier<Block> sample) {
    }

    private final String name;
    private final boolean rotatable;
    private final List<Cell> cells = new ArrayList<>();
    private final List<Supplier<? extends Block>> parts = new ArrayList<>();

    public MultiStructure(String name, boolean rotatable) {
        this.name = name;
        this.rotatable = rotatable;
    }

    public String name() {
        return name;
    }

    /** Registers the block for the next variant number, returning that number. */
    public int addPart(Supplier<? extends Block> block) {
        parts.add(block);
        return parts.size() - 1;
    }

    public Block part(int variant) {
        return parts.get(variant).get();
    }

    public int partCount() {
        return parts.size();
    }

    // ---- building the pattern ----

    /** A cell that needs the structure's block of {@code variant}. */
    public MultiStructure part(int x, int y, int z, int variant) {
        cells.add(new Cell(x, y, z, state -> state.getBlock() == part(variant), false, () -> part(variant)));
        return this;
    }

    /** A cell that needs any of the structure's blocks of the given variants. */
    public MultiStructure partOf(int x, int y, int z, int... variants) {
        cells.add(new Cell(x, y, z, state -> {
            for (int v : variants) {
                if (state.getBlock() == part(v)) {
                    return true;
                }
            }
            return false;
        }, false, () -> part(variants[0])));
        return this;
    }

    /** A cell that needs a particular block (a pipe, say). */
    public MultiStructure block(int x, int y, int z, Supplier<? extends Block> block) {
        cells.add(new Cell(x, y, z, state -> state.getBlock() == block.get(), false, block::get));
        return this;
    }

    /** A cell holding the block entity that runs the structure; it is told when the structure forms or breaks. */
    public MultiStructure controller(int x, int y, int z, Supplier<? extends Block> block) {
        cells.add(new Cell(x, y, z, state -> state.getBlock() == block.get(), true, block::get));
        return this;
    }

    public MultiStructure air(int x, int y, int z) {
        cells.add(new Cell(x, y, z, BlockState::isAir, false, () -> Blocks.AIR));
        return this;
    }

    public MultiStructure any(int x, int y, int z, Predicate<BlockState> match, Supplier<Block> sample) {
        cells.add(new Cell(x, y, z, match, false, sample));
        return this;
    }

    public List<Cell> cells() {
        return cells;
    }

    // ---- checking ----

    /** Offset {@code (x, z)} turned {@code rotation} quarter turns about the origin. */
    private static int rotX(int x, int z, int rotation) {
        return switch (rotation & 3) {
            case 1 -> -z;
            case 2 -> -x;
            case 3 -> z;
            default -> x;
        };
    }

    private static int rotZ(int x, int z, int rotation) {
        return switch (rotation & 3) {
            case 1 -> x;
            case 2 -> -z;
            case 3 -> -x;
            default -> z;
        };
    }

    public static BlockPos at(BlockPos origin, int x, int y, int z, int rotation) {
        return origin.offset(rotX(x, z, rotation), y, rotZ(x, z, rotation));
    }

    /** The way {@code +x} points when the structure is turned {@code rotation} quarter turns. */
    public static Direction forward(int rotation) {
        return switch (rotation & 3) {
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            case 3 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    /** Extra rules beyond the cells (counts of blocks, say). */
    protected boolean extraCheck(Level level, BlockPos origin, int rotation) {
        return true;
    }

    /** Called once the structure has formed, after its parts and controllers have been switched. */
    protected void onFormed(Level level, BlockPos origin, int rotation) {
    }

    public boolean matches(Level level, BlockPos origin, int rotation) {
        for (Cell cell : cells) {
            BlockPos pos = at(origin, cell.x, cell.y, cell.z, rotation);
            if (!level.isLoaded(pos) || !cell.match.test(level.getBlockState(pos))) {
                return false;
            }
        }
        return extraCheck(level, origin, rotation);
    }

    /** Looks for a structure that the block at {@code changed} is part of, and forms it. */
    public boolean tryForm(Level level, BlockPos changed) {
        BlockState state = level.getBlockState(changed);
        for (int rotation = 0; rotation < (rotatable ? 4 : 1); rotation++) {
            for (Cell cell : cells) {
                if (!cell.match.test(state)) {
                    continue;
                }
                BlockPos origin = changed.subtract(new BlockPos(rotX(cell.x, cell.z, rotation), cell.y, rotZ(cell.x, cell.z, rotation)));
                if (matches(level, origin, rotation)) {
                    form(level, origin, rotation);
                    return true;
                }
            }
        }
        return false;
    }

    private void form(Level level, BlockPos origin, int rotation) {
        for (Cell cell : cells) {
            BlockPos pos = at(origin, cell.x, cell.y, cell.z, rotation);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MultiPartBlock part && part.structure() == this && !state.getValue(MultiPartBlock.FORMED)) {
                level.setBlock(pos, state.setValue(MultiPartBlock.FORMED, true), Block.UPDATE_CLIENTS);
            }
        }
        for (Cell cell : cells) {
            if (cell.controller) {
                BlockEntity be = level.getBlockEntity(at(origin, cell.x, cell.y, cell.z, rotation));
                if (be instanceof MultiController controller && controller.structure() == this) {
                    controller.setFormed(true);
                }
            }
        }
        onFormed(level, origin, rotation);
    }

    // ---- breaking ----

    /** Something at {@code pos} (a part, or a controller) is gone: unforms every formed part touching it, and the controllers beside them. */
    public void breakAround(Level level, BlockPos pos) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            queue.add(near.immutable());
            seen.add(near.immutable());
        }
        Set<BlockPos> unformed = new HashSet<>();
        while (!queue.isEmpty() && unformed.size() < MAX_FLOOD) {
            BlockPos at = queue.poll();
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof MultiPartBlock part && part.structure() == this && state.getValue(MultiPartBlock.FORMED)) {
                level.setBlock(at, state.setValue(MultiPartBlock.FORMED, false), Block.UPDATE_CLIENTS);
                unformed.add(at);
                for (BlockPos near : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                    if (seen.add(near.immutable())) {
                        queue.add(near.immutable());
                    }
                }
            }
        }
        Set<BlockPos> notify = new HashSet<>(unformed);
        notify.add(pos);
        for (BlockPos at : unformed) {
            for (BlockPos near : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                notify.add(near.immutable());
            }
        }
        for (BlockPos at : notify) {
            if (level.getBlockEntity(at) instanceof MultiController controller && controller.structure() == this && controller.isFormed()) {
                controller.setFormed(false);
            }
        }
    }
}
