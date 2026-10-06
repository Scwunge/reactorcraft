package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.multi.MultiController;
import net.scwunge.reactorcraft.content.multi.MultiStructure;
import net.scwunge.reactorcraft.content.multi.PowerStructures;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;

/**
 * Flywheel (TileEntityReactorFlywheel): in its disc of casing it takes the speed and torque of the turbine on the side it faces and passes them out of the
 * other side as shaft power. The small turbine's torque is capped at 12.75 kN*m (25.5 kN*m with ammonia); the big turbine's is not. With no turbine
 * driving it, it spins down.
 */
public class ReactorFlywheelBlockEntity extends ReactorMachineBlockEntity implements IShaftPowerOutput, MultiController {
    private static final int MAX_TORQUE = 12750;
    private static final int MAX_TORQUE_AMMONIA = MAX_TORQUE * 2;

    private int omega;
    private int torque;
    private boolean formed;

    public ReactorFlywheelBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.FLYWHEEL.get(), pos, state, 0);
    }

    /** The side it takes power from: the way the block was placed. */
    public Direction facing() {
        Direction look = getBlockState().getValue(ReactorMachineBlock.LOOK);
        return look.getAxis().isHorizontal() ? look : Direction.EAST;
    }

    @Override
    public MultiStructure structure() {
        return PowerStructures.FLYWHEEL;
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public void setFormed(boolean formed) {
        if (this.formed != formed) {
            this.formed = formed;
            if (level != null && !level.isClientSide) {
                FlywheelBlock.showFormed(level, worldPosition, formed);
            }
            markForSync();
        }
    }

    public int omega() {
        return omega;
    }

    public int torque() {
        return torque;
    }

    public long power() {
        return (long) omega * torque;
    }

    @Override
    protected void tickServer() {
        int beforeOmega = omega;
        int beforeTorque = torque;
        if (level.getBlockEntity(worldPosition.relative(facing())) instanceof TurbineCoreBlockEntity turbine) {
            omega = turbine.generatedPower() > 0 ? turbine.omega() : 0;
            torque = turbine instanceof BigTurbineBlockEntity ? turbine.generatedTorque()
                    : Math.min(turbine.generatedTorque(), turbine.isAmmonia() ? MAX_TORQUE_AMMONIA : MAX_TORQUE);
        } else if (omega > 0) {
            omega -= omega / 32 + 1;
        }
        if (omega <= 0) {
            omega = 0;
            torque = 0;
        }
        if (omega != beforeOmega || torque != beforeTorque) {
            markForSync();
        }
    }

    @Override
    protected void tickClient() {
        if (omega > 0) {
            spin((float) (0.2 * Math.pow(Math.log(omega + 1) / Math.log(2), 1.05)) * 6);
        }
    }

    /** It gives power out of the back, but only while its casing stands. */
    @Override
    public int getTorqueOut(Direction side) {
        return formed && side == facing().getOpposite() ? torque : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return formed && side == facing().getOpposite() ? omega : 0;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Omega", omega);
        tag.putInt("Torque", torque);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        omega = tag.getInt("Omega");
        torque = tag.getInt("Torque");
        formed = tag.getBoolean("Formed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Omega", omega);
        tag.putInt("Torque", torque);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        omega = tag.getInt("Omega");
        torque = tag.getInt("Torque");
        formed = tag.getBoolean("Formed");
    }
}
