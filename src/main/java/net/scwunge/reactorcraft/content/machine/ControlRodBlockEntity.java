package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.CoolantState;
import net.scwunge.reactorcraft.core.Linkable;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.ReactorPart;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.TemperaturedReactorTyped;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Control Rod (TileEntityControlRod): a rod that is either fully lowered into the core, where it soaks up 60% of the neutrons
 * that enter it, or raised out of it, where it does nothing. It moves a step a tick; a SCRAM drops it seven times faster.
 * Rods stacked in a column move together.
 */
public class ControlRodBlockEntity extends ReactorMachineBlockEntity implements TemperaturedReactorTyped, Linkable, NeutronTile, ReactorPart {
    private static final int MIN_OFFSET = -5;
    private static final int MAX_OFFSET = 20;

    /** What the rod is doing; the number is how far it moves each tick. */
    public enum Motion {
        RAISING(1),
        LOWERING(-1),
        SCRAM(-7);

        final int step;

        Motion(int step) {
            this.step = step;
        }
    }

    private boolean lowered = true;
    @Nullable
    private Motion motion;
    private int rodOffset = MIN_OFFSET;
    @Nullable
    private BlockPos cpu;

    public ControlRodBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.CONTROL_ROD.get(), pos, state, 0);
    }

    @Override
    protected void tickServer() {
        moveRod();
        if (thermalStep()) {
            updateTemperature();
        }
    }

    @Override
    protected void tickClient() {
        moveRod();
    }

    private void moveRod() {
        if (motion != null) {
            rodOffset += motion.step;
        }
        if (rodOffset <= MIN_OFFSET || rodOffset >= MAX_OFFSET) {
            motion = null;
            rodOffset = Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, rodOffset));
            boolean wasLowered = lowered;
            lowered = rodOffset == MIN_OFFSET;
            if (wasLowered != lowered && level != null && !level.isClientSide) {
                syncToClient();
            }
        }
        if (level != null && !level.isClientSide && getBlockState().hasProperty(ControlRodBlock.ACTIVE) && getBlockState().getValue(ControlRodBlock.ACTIVE) != isActive()) {
            level.setBlock(worldPosition, getBlockState().setValue(ControlRodBlock.ACTIVE, isActive()), 3);
        }
    }

    /** toggle: raises a lowered rod and lowers a raised one, and, if {@code spread}, every rod above and below it in the column. */
    public void toggle(boolean sound, boolean spread) {
        motion = lowered ? Motion.RAISING : Motion.LOWERING;
        if (spread) {
            for (Direction dir : new Direction[]{Direction.UP, Direction.DOWN}) {
                BlockEntity next = level.getBlockEntity(worldPosition.relative(dir));
                while (next instanceof ControlRodBlockEntity rod) {
                    rod.toggle(false, false);
                    next = level.getBlockEntity(rod.worldPosition.relative(dir));
                }
            }
        }
        if (sound) {
            click(1.3F);
        }
        syncToClient();
    }

    public void setActive(boolean active, boolean sound) {
        motion = active ? Motion.LOWERING : Motion.RAISING;
        if (sound) {
            click(1.3F);
        }
        syncToClient();
    }

    /** drop: SCRAM. */
    public void drop(boolean sound) {
        if (sound && rodOffset > MIN_OFFSET && motion != Motion.SCRAM) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1F, 0.5F);
        }
        motion = Motion.SCRAM;
        syncToClient();
    }

    private void click(float pitch) {
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1F, pitch);
    }

    /** Whether it is fully lowered, and so absorbing. */
    public boolean isActive() {
        return lowered && rodOffset == MIN_OFFSET;
    }

    public int rodPosition() {
        return rodOffset;
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        return isActive() && Chance.of(level.random, 60);
    }

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    @Override
    public int getMaxTemperature() {
        return 0;
    }

    @Override
    public boolean canDumpHeatInto(CoolantState coolant) {
        return coolant.isWater();
    }

    @Override
    public ReactorType getReactorType() {
        return ReactorType.FISSION;
    }

    @Override
    public void setRemoved() {
        if (cpu != null && level != null && level.getBlockEntity(cpu) instanceof CpuBlockEntity c) {
            c.layout().remove(worldPosition);
            c.removeTemperatureCheck(worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public void link(BlockPos cpu) {
        this.cpu = cpu;
        setChanged();
    }

    @Nullable
    @Override
    public BlockPos cpu() {
        return cpu;
    }

    // ---- saving and sync ----

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putBoolean("Lowered", lowered);
        tag.putInt("RodOffset", rodOffset);
        if (motion != null) {
            tag.putInt("Motion", motion.ordinal());
        }
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        lowered = tag.getBoolean("Lowered");
        rodOffset = tag.getInt("RodOffset");
        motion = tag.contains("Motion") ? Motion.values()[tag.getInt("Motion")] : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeClientData(tag, registries);
        if (cpu != null) {
            tag.putLong("Cpu", cpu.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readClientData(tag, registries);
        cpu = tag.contains("Cpu") ? BlockPos.of(tag.getLong("Cpu")) : null;
    }
}
