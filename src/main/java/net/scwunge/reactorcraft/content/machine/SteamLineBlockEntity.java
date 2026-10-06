package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.ReactorTypeMix;
import net.scwunge.reactorcraft.core.SteamTile;
import net.scwunge.reactorcraft.core.WorkingFluid;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;

/**
 * Steam Line (TileEntitySteamLine): steam moves along a line of these from a boiler to a grate or turbine, each block passing
 * half of any difference to its neighbour. It remembers which kinds of reactor the steam came from. Too much steam in one
 * block bursts it.
 */
public class SteamLineBlockEntity extends ReactorMachineBlockEntity implements SteamTile {
    private int steam;
    private WorkingFluid fluid = WorkingFluid.EMPTY;
    private final ReactorTypeMix source = new ReactorTypeMix();

    public SteamLineBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.STEAM_LINE.get(), pos, state, 0);
    }

    @Override
    protected void tickServer() {
        drawFromBoiler();
        getPipeSteam();
        if (steam <= 0) {
            fluid = WorkingFluid.EMPTY;
            source.clear();
        } else if (steam > ReactorConfig.STEAM_LINE_CAPACITY.get()) {
            if (WorldSafety.mayChange(level, worldPosition, owner())) {
                level.setBlockAndUpdate(worldPosition, Blocks.AIR.defaultBlockState());
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 2F, true,
                        Level.ExplosionInteraction.BLOCK);
            }
        }
    }

    /** A boiler below gives all its steam, once it has been there a few ticks. */
    private void drawFromBoiler() {
        if (level.getBlockEntity(worldPosition.below()) instanceof ReactorBoilerBlockEntity boiler && boiler.age() > 5
                && canTake(boiler.workingFluid())) {
            fluid = boiler.workingFluid();
            int s = boiler.removeSteam();
            steam += s;
            for (ReactorType type : boiler.reactorTypes()) {
                ReactorType use = type;
                if (use == null || use == ReactorType.NONE) {
                    use = boiler.getReactorType();
                }
                if (use == null || use == ReactorType.NONE) {
                    use = boiler.defaultReactorType();
                }
                source.add(use, s * boiler.reactorTypeFraction(type));
            }
        }
    }

    private boolean canTake(WorkingFluid other) {
        return other != WorkingFluid.EMPTY && (fluid == WorkingFluid.EMPTY || fluid == other);
    }

    private void getPipeSteam() {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof SteamLineBlockEntity other && canTake(other.fluid)) {
                readPipe(other);
            }
        }
    }

    /** Takes half the difference (and one) from a fuller line, and the share of its source types that goes with it. */
    private void readPipe(SteamLineBlockEntity other) {
        int difference = other.steam - steam;
        if (difference > 0) {
            int amount = difference / 2 + 1;
            float fraction = amount / (float) other.steam;
            steam += amount;
            other.steam -= amount;
            fluid = other.fluid;
            addSources(other, fraction);
        }
    }

    private void addSources(SteamLineBlockEntity from, float fraction) {
        for (ReactorType type : from.source.types()) {
            double value = from.source.value(type) * fraction;
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                continue;
            }
            source.add(type, value);
            from.source.add(type, -value);
        }
    }

    @Override
    public int getSteam() {
        return steam;
    }

    public void removeSteam(int amount) {
        steam -= amount;
    }

    public WorkingFluid workingFluid() {
        return fluid;
    }

    public ReactorTypeMix sourceTypes() {
        return source.copy();
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Steam", steam);
        tag.putInt("Working", fluid.ordinal());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        steam = tag.getInt("Steam");
        fluid = WorkingFluid.byId(tag.getInt("Working"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Steam", steam);
        tag.putInt("Working", fluid.ordinal());
        CompoundTag mix = new CompoundTag();
        source.save(mix);
        tag.put("Sources", mix);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam = tag.getInt("Steam");
        fluid = WorkingFluid.byId(tag.getInt("Working"));
        source.load(tag.getCompound("Sources"));
    }
}
