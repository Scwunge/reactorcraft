package net.scwunge.reactorcraft.content.multi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

/** The three fusion structures, laid out as the original's block checks had them. */
public final class FusionStructures {
    /**
     * The solenoid: a 17 by 3 by 17 ring of coil blocks round a solenoid block, spun by shaft power. Variants: 0 and 1 the coil's inner and outer
     * rim, 2 and 3 its middle band, 4 the spokes, 5 the hub. The origin is the solenoid block.
     */
    public static final MultiStructure SOLENOID = new MultiStructure("solenoid", false) {
        {
            for (int i = -5; i <= 5; i++) {
                int d = Math.abs(i) >= 4 ? 7 : 8;
                int rim = Math.abs(i) >= 3 ? 1 : 0;
                int band = Math.abs(i) >= 3 ? 3 : 2;
                for (int dy = -1; dy <= 1; dy++) {
                    int variant = dy == 0 ? band : rim;
                    part(-d, dy, i, variant);
                    part(d, dy, i, variant);
                    part(i, dy, d, variant);
                    part(i, dy, -d, variant);
                }
            }
            part(-6, 1, -6, 1);
            part(-6, -1, -6, 1);
            for (int i = 2; i <= 7; i++) {
                part(i, 0, 0, 4);
                part(-i, 0, 0, 4);
                part(0, 0, i, 4);
                part(0, 0, -i, 4);
                if (i < 6) {
                    part(i, 0, i, 4);
                    part(-i, 0, i, 4);
                    part(i, 0, -i, 4);
                    part(-i, 0, -i, 4);
                }
            }
            for (int i = -1; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    for (int k = -1; k <= 1; k++) {
                        if (i != 0 || j != 0 || k != 0) {
                            part(i, j, k, 5);
                        }
                    }
                }
            }
            controller(0, 0, 0, () -> ReactorBlocks.SOLENOID.get());
        }
    };

    /**
     * The injector's housing: a long box beside the injector and a line of magnetic pipe leading from it. The origin is the bottom of the hub column
     * (variant 5); the structure runs along +x from there, and may stand turned to face any way. Variants: 0 floor, 1 floor edge, 2 wall,
     * 3 roof, 4 roof edge, 5 hub, 6 post, 7 filling.
     */
    public static final MultiStructure INJECTOR = new MultiStructure("injector", true) {
        {
            for (int i = 0; i <= 4; i++) {
                part(i, 0, 3, 3);
                part(i, 1, 3, 4);
                part(i, -1, 3, 4);
            }
            for (int i = 5; i <= 6; i++) {
                part(i, 0, 2, 3);
                part(i, 1, 2, 4);
                part(i, -1, 2, 4);
            }
            for (int i = 7; i <= 8; i++) {
                part(i, 0, 1, 3);
                part(i, 1, 1, 4);
                part(i, -1, 1, 4);
            }
            for (int i = 0; i <= 8; i++) {
                part(i, 0, -1, 0);
                part(i, 1, -1, 1);
                part(i, -1, -1, 1);
            }
            for (int i : new int[]{1, 3, 4, 5, 6, 7}) {
                part(i, 1, 0, 2);
                part(i, -1, 0, 2);
            }
            for (int i = 1; i <= 6; i++) {
                part(i, 1, 1, 2);
                part(i, -1, 1, 2);
            }
            for (int i = 1; i <= 4; i++) {
                part(i, 1, 2, 2);
                part(i, -1, 2, 2);
            }
            for (int u = 0; u <= 2; u++) {
                part(0, 0, u, 5);
                part(0, 1, u, 6);
                part(0, -1, u, 6);
            }
            part(8, 1, 0, 6);
            part(8, -1, 0, 6);
            air(2, 1, 0);
            air(2, -1, 0);
            part(1, 0, 0, 7);
            for (int i = 1; i <= 6; i++) {
                part(i, 0, 1, 7);
            }
            for (int i = 1; i <= 4; i++) {
                part(i, 0, 2, 7);
            }
            for (int i = 3; i <= 8; i++) {
                block(i, 0, 0, () -> ReactorBlocks.MAGNETIC_PIPE.get());
            }
            controller(2, 0, 0, () -> ReactorBlocks.FUSION_INJECTOR.get());
        }

        @Override
        protected void onFormed(Level level, BlockPos origin, int rotation) {
            BlockPos injector = at(origin, 2, 0, 0, rotation);
            BlockState state = level.getBlockState(injector);
            if (state.hasProperty(ReactorMachineBlock.LOOK)) {
                level.setBlock(injector, state.setValue(ReactorMachineBlock.LOOK, forward(rotation)), Block.UPDATE_CLIENTS);
            }
        }
    };

    /**
     * The fusion heater: a 5 by 6 by 5 chamber round the heater block, with a column of magnetic pipe leaving the top. Variants: 0 the lens, 1 chamber
     * lining, 2 corners, 3 edges, 4 walls and floor. The origin is the lowest corner; the heater block is at (2, 2, 2).
     */
    public static final MultiStructure HEATER = new MultiStructure("heater", false) {
        {
            for (int x : new int[]{0, 4}) {
                for (int y : new int[]{0, 4}) {
                    for (int z : new int[]{0, 4}) {
                        part(x, y, z, 2);
                    }
                }
            }
            for (int i = 1; i <= 3; i++) {
                for (int a : new int[]{0, 4}) {
                    for (int b : new int[]{0, 4}) {
                        part(i, a, b, 3);
                        part(a, i, b, 3);
                        part(a, b, i, 3);
                    }
                }
            }
            for (int i = 1; i <= 3; i++) {
                for (int k = 1; k <= 3; k++) {
                    if (i == 2 && k == 2) {
                        continue;
                    }
                    part(i, 0, k, 4);
                    part(i, 4, k, 1);
                    part(i, 5, k, i == 2 || k == 2 ? 3 : 2);
                    part(i, k, 0, 4);
                    part(i, k, 4, 4);
                    part(0, k, i, 4);
                    part(4, k, i, 4);
                }
            }
            for (int y = 3; y <= 6; y++) {
                block(2, y, 2, () -> ReactorBlocks.MAGNETIC_PIPE.get());
            }
            controller(2, 2, 2, () -> ReactorBlocks.FUSION_HEATER.get());
            for (int i = 1; i <= 3; i++) {
                for (int j = 1; j <= 3; j++) {
                    for (int k = 1; k <= 3; k++) {
                        if (i == 2 && j == 2 && k == 2 || i == 2 && j == 3 && k == 2) {
                            continue;
                        }
                        if (j == 2) {
                            any(i, j, k, state -> isLining(state) || state.getBlock() instanceof net.scwunge.rotarycraft.block.PipeBlock, () -> part(1));
                        } else {
                            any(i, j, k, FusionStructures::isLining, () -> part(1));
                        }
                    }
                }
            }
        }

        @Override
        protected boolean extraCheck(Level level, BlockPos origin, int rotation) {
            int total = 0;
            int lenses = 0;
            for (int i = 1; i <= 3; i++) {
                for (int j = 1; j <= 3; j++) {
                    for (int k = 1; k <= 3; k++) {
                        BlockState state = level.getBlockState(origin.offset(i, j, k));
                        if (state.getBlock() == part(0)) {
                            total++;
                            lenses++;
                        } else if (state.getBlock() == part(1)) {
                            total++;
                        }
                    }
                }
            }
            return total >= 22 && lenses == 1;
        }
    };

    private static boolean isLining(BlockState state) {
        return state.getBlock() == HEATER.part(0) || state.getBlock() == HEATER.part(1);
    }

    private FusionStructures() {
    }
}
