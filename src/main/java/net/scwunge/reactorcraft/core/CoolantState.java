package net.scwunge.reactorcraft.core;

/** What a coolant cell is full of (the original's LiquidStates). The fluids themselves are looked up by the cell. */
public enum CoolantState implements net.minecraft.util.StringRepresentable {
    EMPTY,
    WATER,
    HEAVY,
    SODIUM,
    LITHIUM;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    public boolean isWater() {
        return this == WATER || this == HEAVY;
    }
}
