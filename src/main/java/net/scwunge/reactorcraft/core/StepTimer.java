package net.scwunge.reactorcraft.core;

/** DragonAPI's StepTimer: counts up to a cap; {@link #checkCap} reports reaching it and starts over. */
public final class StepTimer {
    private int value;
    private int cap;

    public StepTimer(int cap) {
        this.cap = cap;
    }

    public StepTimer setCap(int cap) {
        this.cap = cap;
        return this;
    }

    public void update() {
        value++;
    }

    public void update(int time) {
        value += time;
    }

    public boolean isAtCap() {
        return value >= cap;
    }

    public boolean checkCap() {
        boolean at = isAtCap();
        if (at) {
            reset();
        }
        return at;
    }

    public void reset() {
        value = 0;
    }

    public void setTick(int tick) {
        value = tick;
    }

    public int getTick() {
        return value;
    }

    public int getCap() {
        return cap;
    }

    public float fraction() {
        return cap <= 0 ? 0 : (float) value / cap;
    }
}
