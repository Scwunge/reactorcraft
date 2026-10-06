package net.scwunge.reactorcraft.content.material;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** The eight fluorite colours (FluoriteTypes), with the original's light colours. */
public enum FluoriteColor implements StringRepresentable {
    BLUE(0, 38, 255),
    PINK(255, 255, 236),
    ORANGE(255, 155, 0),
    MAGENTA(178, 0, 255),
    GREEN(0, 188, 18),
    RED(255, 50, 50),
    WHITE(255, 255, 255),
    YELLOW(255, 216, 0);

    public final int red;
    public final int green;
    public final int blue;

    FluoriteColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int rgb() {
        return red << 16 | green << 8 | blue;
    }
}
