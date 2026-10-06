package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.block.SteamBlock;
import net.scwunge.reactorcraft.core.SteamTile;
import net.scwunge.reactorcraft.core.WorkingFluid;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

/**
 * Steam Grate (TileEntitySteamGrate): takes steam from the lines around it and lets it out above as steam blocks, one steam
 * unit for each, which then rise towards a turbine. It can be set (shift-right-click) to work only while it is powered by
 * redstone, or only while it is not.
 */
public class SteamGrateBlockEntity extends ReactorMachineBlockEntity implements SteamTile {
    private int steam;
    private boolean requireRedstone;
    private WorkingFluid fluid = WorkingFluid.EMPTY;

    public SteamGrateBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.STEAM_GRATE.get(), pos, state, 0);
    }

    @Override
    protected void tickServer() {
        draw();
        if (canMakeSteam()) {
            steam--;
            level.setBlock(worldPosition.above(), SteamBlock.grateSteam(fluid == WorkingFluid.AMMONIA), 3);
        }
        if (steam <= 0) {
            fluid = WorkingFluid.EMPTY;
        }
    }

    private boolean canMakeSteam() {
        return steam > 0 && level.hasNeighborSignal(worldPosition) == requireRedstone
                && ((SteamBlock) ReactorBlocks.STEAM.get()).canMoveInto(level, worldPosition.above());
    }

    private boolean canTake(WorkingFluid other) {
        return other != WorkingFluid.EMPTY && (fluid == WorkingFluid.EMPTY || fluid == other);
    }

    /** A quarter of the difference (and one) from any steam line beside it. */
    private void draw() {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof SteamLineBlockEntity line && canTake(line.workingFluid())) {
                fluid = line.workingFluid();
                int ds = line.getSteam() - steam;
                if (ds > 0) {
                    int amount = ds / 4 + 1;
                    steam += amount;
                    line.removeSteam(amount);
                }
            }
        }
    }

    /** Toggles whether it needs a redstone signal. */
    public boolean toggleRequireRedstone() {
        requireRedstone = !requireRedstone;
        setChanged();
        return requireRedstone;
    }

    @Override
    public int getSteam() {
        return steam;
    }

    public boolean requiresRedstone() {
        return requireRedstone;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Steam", steam);
        tag.putInt("Working", fluid.ordinal());
        tag.putBoolean("RequireRedstone", requireRedstone);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam = tag.getInt("Steam");
        fluid = WorkingFluid.byId(tag.getInt("Working"));
        requireRedstone = tag.getBoolean("RequireRedstone");
    }
}
