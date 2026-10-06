package net.scwunge.reactorcraft.core;

/** A reactor part with a temperature that neighbours can push heat into or take it from. */
public interface Temperatured {
    int getTemperature();

    void setTemperature(int temperature);

    int getMaxTemperature();

    /** Whether this part may give heat to a coolant cell holding {@code coolant}. */
    boolean canDumpHeatInto(CoolantState coolant);
}
