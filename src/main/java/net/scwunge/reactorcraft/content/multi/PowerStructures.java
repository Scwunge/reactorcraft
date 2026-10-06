package net.scwunge.reactorcraft.content.multi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.machine.BigTurbineBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorFlywheelBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorGeneratorBlockEntity;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The structures round the big turbine, the flywheel and the generator, laid out as the original's BlockTurbineMulti, FlywheelStructure and GeneratorStructure had them. */
public final class PowerStructures {
    private PowerStructures() {}

    /** The turbine's casing, widest at the far end: one 11 by 11 slice for each stage ('t' casing, 'h' housing, 'e' injector, anything else free). */
    private static final String[][] SLICES = {
            {"xbbhhhhhbbx", "bbhhhthhhbb", "bhhttttthhb", "hhttttttthh", "hhttttttthh", "httttxtttth", "hhttttttthh", "hhttttttthh", "bhhttttthhb", "bbhhhthhhbb", "xbbhhhhhbbx"},
            {"xxbbbbbbbxx", "xbbhhhhhbbx", "bbhhttthhbb", "bhhttttthhb", "bhttttttthb", "bhtttxttthb", "bhttttttthb", "bhhttttthhb", "bbhhttthhbb", "xbbhhhhhbbx", "xxbbbbbbbxx"},
            {"xxxbbbbbxxx", "xbbbhhhbbbx", "xbhhhhhhhbx", "bbhhttthhbb", "bhhttttthhb", "bhhttxtthhb", "bhhttttthhb", "bbhhttthhbb", "xbhhhhhhhbx", "xbbbhhhbbbx", "xxxbbbbbxxx"},
            {"xxxxxxxxxxx", "xxbbbbbbbxx", "xbbhhhhhbbx", "xbhhttthhbx", "xbhttttthbx", "xbhttxtthbx", "xbhttttthbx", "xbhhttthhbx", "xbbhhhhhbbx", "xxbbbbbbbxx", "xxxxxxxxxxx"},
            {"xxxxxxxxxxx", "xxxbbbbbxxx", "xxbbhhhbbxx", "xbbhhhhhbbx", "xbhhttthhbx", "xbhhtxthhbx", "xbhhttthhbx", "xbbhhhhhbbx", "xxbbhhhbbxx", "xxxbbbbbxxx", "xxxxxxxxxxx"},
            {"xxxxxxxxxxx", "xxxxxxxxxxx", "xxbbbbbbbxx", "xxbhhhhhbxx", "xxbhhthhbxx", "xxbhtxthbxx", "xxbhhthhbxx", "xxbhhhhhbxx", "xxbbbbbbbxx", "xxxxxxxxxxx", "xxxxxxxxxxx"},
            {"xxxxxxxxxxx", "xxxxxxxxxxx", "xxxbbbbbxxx", "xxbbhhhbbxx", "xxbhhhhhbxx", "xxbhhxhhbxx", "xxbhhhhhbxx", "xxbbhhhbbxx", "xxxbbbbbxxx", "xxxxxxxxxxx", "xxxxxxxxxxx"},
            {"xxxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", "xxxbbbbbxxx", "xxxbeeebxxx", "xxxbexebxxx", "xxxbeeebxxx", "xxxbbbbbxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx"}
    };
    public static final int MAX_STAGES = 7;

    /**
     * The big turbine: up to seven turbine blocks in a line (the origin is the first, where steam goes in; the structure runs along +x, the way the steam
     * moves, and may stand turned to any side), each in a casing that is wider the further along it is, with a ring of steam injectors round the block
     * before the first. Variants: 0 casing, 1 housing, 2 steam injector. It forms with however many turbine blocks there are, from one to seven.
     */
    public static final TurbineStructure TURBINE = new TurbineStructure();

    public static final class TurbineStructure extends MultiStructure {
        /** For each cell, which stage's casing it belongs to (-1 for the injector ring, which every size needs). */
        private final List<Integer> stages = new ArrayList<>();

        TurbineStructure() {
            super("turbine", true);
            for (int stage = 0; stage < MAX_STAGES; stage++) {
                String[] slice = SLICES[MAX_STAGES - 1 - stage];
                int before = cells().size();
                addSlice(stage, slice);
                controller(stage, 0, 0, () -> ReactorBlocks.BIG_TURBINE.get());
                for (int i = before; i < cells().size(); i++) {
                    stages.add(stage);
                }
            }
            int before = cells().size();
            addSlice(-1, SLICES[MAX_STAGES]);
            for (int i = before; i < cells().size(); i++) {
                stages.add(-1);
            }
        }

        private void addSlice(int stage, String[] slice) {
            int x = stage < 0 ? -1 : stage;
            for (int row = 0; row < 11; row++) {
                for (int col = 0; col < 11; col++) {
                    int variant = switch (slice[row].charAt(col)) {
                        case 't' -> 0;
                        case 'h' -> 1;
                        case 'e' -> 2;
                        default -> -1;
                    };
                    if (variant >= 0) {
                        part(x, 5 - row, col - 5, variant);
                    }
                }
            }
        }

        /** Which stage's casing cell number {@code index} is part of (-1 for the injector ring). */
        public int stageOf(int index) {
            return stages.get(index);
        }

        /** How many turbine blocks run along +x from the origin (the way this structure stands), or 0 if the origin is not the first of a line. */
        public int stagesAt(Level level, BlockPos origin, int rotation) {
            Direction forward = forward(rotation);
            if (level.getBlockEntity(at(origin, -1, 0, 0, rotation)) instanceof BigTurbineBlockEntity) {
                return 0;
            }
            int n = 0;
            while (n < MAX_STAGES && level.getBlockEntity(at(origin, n, 0, 0, rotation)) instanceof BigTurbineBlockEntity turbine && turbine.steamMovement() == forward) {
                n++;
            }
            return n;
        }

        @Override
        public boolean matches(Level level, BlockPos origin, int rotation) {
            int n = stagesAt(level, origin, rotation);
            if (n == 0) {
                return false;
            }
            for (int i = 0; i < cells().size(); i++) {
                Cell cell = cells().get(i);
                if (stages.get(i) >= n) {
                    continue;
                }
                BlockPos pos = at(origin, cell.x(), cell.y(), cell.z(), rotation);
                if (!level.isLoaded(pos) || !cell.match().test(level.getBlockState(pos))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void form(Level level, BlockPos origin, int rotation) {
            int n = stagesAt(level, origin, rotation);
            for (int i = 0; i < cells().size(); i++) {
                Cell cell = cells().get(i);
                if (stages.get(i) >= n) {
                    continue;
                }
                BlockPos pos = at(origin, cell.x(), cell.y(), cell.z(), rotation);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof MultiPartBlock part && part.structure() == this && !state.getValue(MultiPartBlock.FORMED)) {
                    level.setBlock(pos, state.setValue(MultiPartBlock.FORMED, true), Block.UPDATE_CLIENTS);
                }
                if (cell.controller() && level.getBlockEntity(pos) instanceof MultiController controller && controller.structure() == this) {
                    controller.setFormed(true);
                }
            }
        }
    }

    /**
     * The flywheel: a disc of casing standing round the flywheel block, in the plane across the way it faces (the structure's +x). Variants: 0 and 1 the
     * spokes, 2 the rim.
     */
    public static final MultiStructure FLYWHEEL = new MultiStructure("flywheel", true) {
        {
            for (int i = 1; i <= 2; i++) {
                int v = i == 1 ? 0 : 2;
                part(0, 0, i, v);
                part(0, 0, -i, v);
                part(0, -i, 0, v);
                part(0, i, 0, v);
            }
            for (int s = -1; s <= 1; s += 2) {
                part(0, 1, s, 1);
                part(0, -1, s, 1);
                part(0, 2, s, 2);
                part(0, -2, s, 2);
                part(0, 1, 2 * s, 2);
                part(0, -1, 2 * s, 2);
            }
            controller(0, 0, 0, () -> ReactorBlocks.FLYWHEEL.get());
        }

        @Override
        protected boolean extraCheck(Level level, BlockPos origin, int rotation) {
            return level.getBlockEntity(origin) instanceof ReactorFlywheelBlockEntity flywheel && flywheel.facing().getAxis() == forward(rotation).getAxis();
        }
    };

    /**
     * The generator: the generator block at the origin, then a long housing of nine blocks along +x, which ends at the turbine's last stage (or its flywheel
     * position) ten blocks from the generator block. Variants: 0 the axle, 1 and 3 the windings (3 at the turbine end, 1 beyond), 2 the frame.
     */
    public static final MultiStructure GENERATOR = new MultiStructure("generator", true) {
        {
            Map<Long, Integer> grid = new LinkedHashMap<>();
            int l = ReactorGeneratorBlockEntity.LENGTH - 1;
            // cell i runs 0..l-1 from the turbine end: along +x from the generator block that is 9 - i... x = l - i
            for (int i = 0; i < l; i++) {
                int x = l - i;
                int winding = i < 2 ? 3 : 1;
                for (int k = -1; k <= 1; k++) {
                    for (int m = -1; m <= 1; m++) {
                        put(grid, x, k, m, winding);
                    }
                }
            }
            for (int i = 0; i < l; i++) {
                int x = l - i;
                int variant;
                for (int k = -2; k <= 2; k += 4) {
                    variant = i == 1 && k == 2 ? 3 : 2;
                    put(grid, x, k, 1, 2);
                    put(grid, x, k, -1, 2);
                    put(grid, x, k, 0, variant);
                }
                for (int k = -1; k <= 1; k++) {
                    put(grid, x, k, 2, 2);
                    put(grid, x, k, -2, 2);
                }
            }
            for (int k = -2; k <= 2; k++) {
                for (int m = -2; m <= 2; m++) {
                    if ((Math.abs(k) != 2 || Math.abs(m) != 2) && (k != 0 || m != 0)) {
                        put(grid, 0, k, m, 2);
                    }
                }
            }
            for (int i = 0; i < 2; i++) {
                int x = l - i;
                put(grid, x, 2, 2, 2);
                put(grid, x, 2, -2, 2);
                put(grid, x, -2, 2, 2);
                put(grid, x, -2, -2, 2);
            }
            for (int i = 0; i < l; i++) {
                put(grid, l - i, 0, 0, 0);
            }
            for (Map.Entry<Long, Integer> e : grid.entrySet()) {
                long key = e.getKey();
                part((int) (key >> 40), (int) ((key >> 20) & 0xFFFFF) - 512, (int) (key & 0xFFFFF) - 512, e.getValue());
            }
            controller(0, 0, 0, () -> ReactorBlocks.GENERATOR.get());
        }

        private void put(Map<Long, Integer> grid, int x, int y, int z, int variant) {
            grid.put(((long) x << 40) | ((long) (y + 512) << 20) | (z + 512), variant);
        }

        @Override
        protected boolean extraCheck(Level level, BlockPos origin, int rotation) {
            BlockEntity be = level.getBlockEntity(origin);
            return be instanceof ReactorGeneratorBlockEntity generator && generator.facing() == forward(rotation);
        }
    };
}
