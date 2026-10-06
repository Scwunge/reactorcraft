package net.scwunge.reactorcraft.core;

/**
 * The 32 directions a toroid magnet can steer plasma towards (the original's Aim): the angle it is drawn at, and where the next magnet
 * round the ring is, in blocks across and along from it.
 */
public enum ToroidAim {
    N(0, 2, 0),
    NNW1(11.3F, 2, -1),
    NNW2(24, 2, -1),
    NNW3(36.9F, 2, -2),
    NW(45, 2, -2),
    WNW1(53.1F, 1, -2),
    WNW2(66, 1, -2),
    WNW3(78.7F, 0, -2),
    W(90, 0, -2),
    WSW1(101.3F, -1, -2),
    WSW2(114, -1, -2),
    WSW3(126.9F, -2, -2),
    SW(135, -2, -2),
    SSW1(143.1F, -2, -1),
    SSW2(156, -2, -1),
    SSW3(168.7F, -2, 0),
    S(180, -2, 0),
    SSE1(191.3F, -2, 1),
    SSE2(204, -2, 1),
    SSE3(216.9F, -2, 2),
    SE(225, -2, 2),
    ESE1(233.1F, -1, 2),
    ESE2(246, -1, 2),
    ESE3(258.7F, 0, 2),
    E(270, 0, 2),
    ENE1(281.3F, 1, 2),
    ENE2(294, 1, 2),
    ENE3(306.9F, 2, 2),
    NE(315, 2, 2),
    NNE1(323.1F, 2, 1),
    NNE2(336, 2, 1),
    NNE3(348.7F, 2, 0);

    public static final ToroidAim[] LIST = values();

    public final float angle;
    public final int xOffset;
    public final int zOffset;

    ToroidAim(float angle, int x, int z) {
        this.angle = angle;
        this.xOffset = x;
        this.zOffset = z;
    }

    public ToroidAim next() {
        return LIST[(ordinal() + 1) % LIST.length];
    }

    public ToroidAim previous() {
        return LIST[(ordinal() + LIST.length - 1) % LIST.length];
    }

    public boolean isCardinal() {
        return ordinal() % 8 == 0;
    }

    public static ToroidAim byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < LIST.length ? LIST[ordinal] : N;
    }
}
