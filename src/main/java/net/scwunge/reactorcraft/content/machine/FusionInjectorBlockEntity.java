package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.content.entity.PlasmaEntity;
import net.scwunge.reactorcraft.content.multi.FusionStructures;
import net.scwunge.reactorcraft.content.multi.MultiController;
import net.scwunge.reactorcraft.content.multi.MultiStructure;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.ToroidPart;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Fusion Injector (TileEntityFusionInjector): once built into its structure it feeds fusion plasma, from a magnetic pipe, into the ring, one bit of
 * plasma a tick for every two mB, flying the way the block faces. A redstone signal stops it.
 */
public class FusionInjectorBlockEntity extends ReactorMachineBlockEntity implements ToroidPart, NeutronTile, PlasmaPort, MultiController {
    public static final int CAPACITY = 8000;
    public static final int PLASMA_COST = 2;

    private final FluidTank tank;
    private final IFluidHandler plasmaIn;
    private boolean formed;
    private boolean enabled = true;

    public FusionInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.FUSION_INJECTOR.get(), pos, state, 0);
        tank = addTank("Plasma", CAPACITY, s -> s.getFluid() == ReactorFluids.FUSION_PLASMA.get());
        plasmaIn = FluidAccess.fillOnly(tank);
    }

    public Direction facing() {
        return getBlockState().getValue(ReactorMachineBlock.LOOK);
    }

    @Override
    public MultiStructure structure() {
        return FusionStructures.INJECTOR;
    }

    @Override
    protected void tickServer() {
        if (!formed && level.getGameTime() % 40 == Math.floorMod(worldPosition.hashCode(), 40)) {
            structure().tryForm(level, worldPosition);
        }
        if (canMake()) {
            PlasmaEntity plasma = new PlasmaEntity(level, worldPosition, owner());
            Direction face = facing();
            plasma.setTarget(worldPosition.getX() + face.getStepX(), worldPosition.getZ() + face.getStepZ());
            level.addFreshEntity(plasma);
            removeLiquid(tank, PLASMA_COST);
        }
    }

    public boolean canMake() {
        return formed && enabled && !tank.isEmpty() && !level.hasNeighborSignal(worldPosition);
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public void setFormed(boolean formed) {
        if (this.formed != formed) {
            this.formed = formed;
            markForSync();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        markForSync();
    }

    public FluidTank plasmaTank() {
        return tank;
    }

    @Nullable
    @Override
    public ToroidPart nextPart(Level level) {
        Direction face = facing();
        return level.getBlockEntity(worldPosition.relative(face, 2)) instanceof ToroidPart part ? part : null;
    }

    /** Plasma comes in from a magnetic pipe, and only from one. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null || level != null && level.getBlockEntity(worldPosition.relative(side)) instanceof MagneticPipeBlockEntity) {
            return plasmaIn;
        }
        return null;
    }

    @Override
    public boolean onNeutron(net.scwunge.reactorcraft.content.entity.NeutronEntity neutron, Level level, BlockPos pos) {
        return false;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("Enabled", enabled);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        formed = tag.getBoolean("Formed");
        enabled = tag.getBoolean("Enabled");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("Enabled", enabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    }
}
