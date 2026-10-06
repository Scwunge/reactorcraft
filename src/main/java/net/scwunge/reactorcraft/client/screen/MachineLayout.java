package net.scwunge.reactorcraft.client.screen;

import java.util.List;
import java.util.Map;

/**
 * Where a machine's GUI puts its tanks and progress bars, from the original's GUI classes. Positions are in pixels from
 * the top left of the 176-wide panel; texture coordinates are in the 256x256 GUI texture.
 */
public record MachineLayout(int width, int height, boolean inventoryLabel, List<TankBox> tanks, List<Bar> bars, List<Overlay> overlays) {
    public MachineLayout(int height, boolean inventoryLabel, List<TankBox> tanks, List<Bar> bars, List<Overlay> overlays) {
        this(176, height, inventoryLabel, tanks, bars, overlays);
    }

    /** A fluid gauge showing tank number {@code tank}. */
    public record TankBox(int tank, int x, int y, int width, int height) {
        public TankBox(int tank, int x, int y) {
            this(tank, x, y, 16, 60);
        }
    }

    /**
     * A progress bar, drawn from the texture at (u, v) as big as {@code size * tick / cap} in its direction. When {@code itemSlot}
     * is not negative the source row is {@code vWithItem} while that slot holds something, else {@code v}.
     */
    public record Bar(int tickValue, int capValue, int x, int y, int width, int height, int u, int v, boolean horizontal, int itemSlot,
                      int vWithItem) {
        public Bar(int tickValue, int capValue, int x, int y, int width, int height, int u, int v, boolean horizontal) {
            this(tickValue, capValue, x, y, width, height, u, v, horizontal, -1, v);
        }
    }

    /** A piece of the GUI texture drawn over the gauges (the centrifuge's tank frame). */
    public record Overlay(int x, int y, int width, int height, int u, int v) {
    }

    private static MachineLayout plain(int height, boolean label) {
        return new MachineLayout(height, label, List.of(), List.of(), List.of());
    }

    /** Layouts by the machine block's name. */
    public static final Map<String, MachineLayout> BY_BLOCK = Map.ofEntries(

            Map.entry("isotope_centrifuge", new MachineLayout(166, false,
                    List.of(new TankBox(0, 80, 18)),
                    // the bar grows downwards from the top, 48 pixels long
                    List.of(new Bar(0, 1, 104, 18, 4, 48, 216, 84, false)),
                    List.of(new Overlay(80, 18, 16, 60, 223, 83)))),
            Map.entry("uranium_processor", new MachineLayout(175, true,
                    List.of(new TankBox(0, 98, 18), new TankBox(1, 116, 18), new TankBox(2, 134, 18)),
                    List.of(new Bar(0, 1, 67, 21, 24, 17, 176, 92, true), new Bar(2, 3, 67, 58, 24, 17, 176, 92, true)),
                    List.of())),
            Map.entry("electrolyzer", new MachineLayout(175, true,
                    // tanks are heavy, light, input in the machine; the gauges put them at the right, middle and left
                    List.of(new TankBox(0, 98, 18), new TankBox(1, 134, 18), new TankBox(2, 17, 18)),
                    List.of(new Bar(1, 2, 65, 17, 66, 62, 177, 124, true, 0, 61)),
                    List.of())),
            Map.entry("fluid_synthesizer", new MachineLayout(175, true,
                    List.of(new TankBox(0, 17, 18), new TankBox(1, 134, 18)),
                    List.of(new Bar(1, 2, 103, 26, 24, 34, 176, 92, true)),
                    List.of())),
            Map.entry("fuel_rod", plain(182, true)),
            Map.entry("breeder_core", plain(182, true)),
            Map.entry("thorium_core", new MachineLayout(100, false,
                    List.of(new TankBox(0, 53, 23, 16, 70), new TankBox(1, 80, 23, 16, 70), new TankBox(2, 107, 23, 16, 70)), List.of(), List.of())),
            Map.entry("pebble_bed", new MachineLayout(240, 237, false, List.of(), List.of(), List.of())),
            Map.entry("waste_container", plain(175, true)),
            Map.entry("waste_decayer", plain(175, true)),
            Map.entry("waste_storage", plain(186, true)));
}