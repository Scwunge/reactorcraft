package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;

/**
 * Shaft power arriving at a machine (the original's ShaftPowerReceiver fields plus PowerTransferHelper.checkPowerFrom):
 * torque, speed and power read once a tick from the first of the given sides that delivers any.
 */
public final class ShaftInput {
    private int torque;
    private int omega;
    private long power;

    /** Reads from the sides in order and keeps the first that delivers power; clears everything if none does. */
    public boolean read(Level level, BlockPos pos, Direction... sides) {
        for (Direction side : sides) {
            IShaftPowerOutput.Reading in = IShaftPowerOutput.readInput(level, pos, side);
            if (in.power() > 0) {
                torque = in.torque();
                omega = in.omega();
                power = in.power();
                return true;
            }
        }
        clear();
        return false;
    }

    public void clear() {
        torque = 0;
        omega = 0;
        power = 0;
    }

    public void set(int torque, int omega) {
        this.torque = torque;
        this.omega = omega;
        this.power = (long) torque * omega;
    }

    public int torque() {
        return torque;
    }

    public int omega() {
        return omega;
    }

    public long power() {
        return power;
    }

    /** TankedReactorPowerReceiver.hasSufficientPower and friends: every minimum met. */
    public boolean meets(long minPower, int minSpeed, int minTorque) {
        return power >= minPower && omega >= minSpeed && torque >= minTorque;
    }

    public void save(CompoundTag tag) {
        tag.putInt("Torque", torque);
        tag.putInt("Omega", omega);
    }

    public void load(CompoundTag tag) {
        set(tag.getInt("Torque"), tag.getInt("Omega"));
    }
}
