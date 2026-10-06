package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.multi.MultiController;
import net.scwunge.reactorcraft.content.multi.MultiStructure;
import net.scwunge.reactorcraft.content.multi.PowerStructures;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.rotarycraft.config.RotaryConfig;

/**
 * Generator (TileEntityReactorGenerator): the block at the end of a long housing whose other end is ten blocks away at the turbine's last stage. While the
 * housing stands it takes the turbine's power and sends it out of its back as Forge Energy (the original also had EU and ElectriCraft modes; here it is FE,
 * at the same rate the Generator of RotaryCraft converts shaft power), every tick, to whatever takes it there. The torque is averaged over a second, which
 * smooths the turbine's flutter. Breaking the housing while it is turning fast blows the generator up.
 */
public class ReactorGeneratorBlockEntity extends ReactorMachineBlockEntity implements MultiController {
    public static final int LENGTH = 10;

    private boolean formed;
    private int omegaIn;
    private int torqueIn;
    private long power;
    private final int[] torqueHistory = new int[20];
    private int historyAt;
    private double average;

    public ReactorGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.GENERATOR.get(), pos, state, 0);
    }

    /** The way the turbine lies: the way the block was placed. */
    public Direction facing() {
        Direction look = getBlockState().getValue(ReactorMachineBlock.LOOK);
        return look.getAxis().isHorizontal() ? look : Direction.EAST;
    }

    @Override
    public MultiStructure structure() {
        return PowerStructures.GENERATOR;
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public void setFormed(boolean formed) {
        if (this.formed && !formed && omegaIn > 1024) {
            fail();
            return;
        }
        if (this.formed != formed) {
            this.formed = formed;
            if (level != null && !level.isClientSide) {
                FlywheelBlock.showFormed(level, worldPosition, formed);
            }
            markForSync();
        }
    }

    private void fail() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean destroy = ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner());
        Level world = level;
        BlockPos at = worldPosition;
        Direction f = facing();
        int l = LENGTH / 2;
        world.removeBlock(at, false);
        world.explode(null, at.getX() + 0.5 + f.getStepX() * l, at.getY() + 0.5, at.getZ() + 0.5 + f.getStepZ() * l, 8F,
                destroy ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /** The turbine ten blocks along the way it faces, if it runs towards the generator. */
    private TurbineCoreBlockEntity turbine() {
        BlockPos at = worldPosition.relative(facing(), LENGTH);
        if (level.isLoaded(at) && level.getBlockEntity(at) instanceof TurbineCoreBlockEntity turbine && turbine.steamMovement() == facing().getOpposite()) {
            return turbine;
        }
        return null;
    }

    /** Power in, in watts. */
    public long power() {
        return power;
    }

    public int omega() {
        return omegaIn;
    }

    /** What the average torque is. */
    public int torque() {
        return (int) average;
    }

    /** getGenUnits: FE a tick it makes. */
    public double generatedUnits() {
        return (double) power / Math.max(1, RotaryConfig.get(RotaryConfig.WATTS_PER_FE));
    }

    @Override
    protected void tickServer() {
        int beforeOmega = omegaIn;
        long beforePower = power;
        TurbineCoreBlockEntity turbine = formed ? turbine() : null;
        if (turbine != null) {
            omegaIn = turbine.generatedPower() > 0 ? Math.min(turbine.omega(), (int) (BigTurbineBlockEntity.GEN_OMEGA * 0.995)) : 0;
            torqueIn = turbine.generatedTorque();
        } else {
            omegaIn = 0;
            torqueIn = 0;
        }
        torqueHistory[historyAt] = torqueIn;
        historyAt = (historyAt + 1) % torqueHistory.length;
        long sum = 0;
        for (int t : torqueHistory) {
            sum += t;
        }
        double now = sum / (double) torqueHistory.length;
        // the original leaves the average alone while it moves by less than 5%
        if (Math.abs(now - average) > Math.min(average, now) * 0.05) {
            average = now;
        }
        power = (long) omegaIn * torqueIn;
        if (power > 0) {
            Direction back = facing().getOpposite();
            IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(back), facing());
            if (receiver != null) {
                receiver.receiveEnergy((int) Math.min(Integer.MAX_VALUE, (long) generatedUnits()), false);
            }
        }
        if (omegaIn != beforeOmega || power != beforePower) {
            markForSync();
        }
    }

    @Override
    protected void tickClient() {
        if (omegaIn > 0) {
            spin((float) (0.5 * Math.pow(Math.log(omegaIn + 1) / Math.log(2), 1.05)));
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Omega", omegaIn);
        tag.putLong("Power", power);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        omegaIn = tag.getInt("Omega");
        power = tag.getLong("Power");
        formed = tag.getBoolean("Formed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putInt("Omega", omegaIn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        omegaIn = tag.getInt("Omega");
    }
}
