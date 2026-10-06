package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.multi.FusionStructures;
import net.scwunge.reactorcraft.content.multi.MultiController;
import net.scwunge.reactorcraft.content.multi.MultiStructure;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.ToroidAim;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;

/**
 * Solenoid Magnet (TileEntitySolenoidMagnet): the middle of the big solenoid coil. Driven from below with 32768 Nm and 256 rad/s or more it spins up,
 * and while it is spinning it tells every magnet of the ring (starting 14 blocks east and 2 north of it) that the ring has a solenoid. Breaking the coil
 * while it is turning fast, or driving it past 8192 rad/s, wrecks it.
 */
public class SolenoidBlockEntity extends ReactorMachineBlockEntity implements MultiController, NeutronTile {
    public static final int MIN_OMEGA = 256;
    public static final int MAX_OMEGA = 8192;
    public static final int MIN_TORQUE = 32768;
    private static final int MAX_SAFE_SPEED = 30;
    /** Rad/s above which pulling the coil apart wrecks it. */
    private static final int BREAKAGE_OMEGA = 32;

    private final ShaftInput shaft = new ShaftInput();
    private boolean formed;
    private boolean checkForToroids = true;
    private float speed;
    private long age;

    public SolenoidBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.SOLENOID.get(), pos, state, 0);
    }

    @Override
    protected void tickServer() {
        age++;
        int omega = shaft.omega();
        int torque = shaft.torque();
        shaft.read(level, worldPosition, Direction.DOWN);
        if (omega != shaft.omega() || torque != shaft.torque()) {
            markForSync();
        }
        speed = canTurn() ? Math.min(speed + (speed > MAX_SAFE_SPEED ? 0.3F : 0.1F), maxRenderSpeed()) : Math.max(0, speed - 0.1F);
        if (!formed && age % 40 == 0) {
            structure().tryForm(level, worldPosition);
        }
        if (formed && arePowerRequirementsMet()) {
            if (checkForToroids) {
                setToroids(true);
                checkForToroids = false;
            }
        } else if (!checkForToroids) {
            setToroids(false);
            checkForToroids = true;
        }
        if (formed && shaft.torque() >= MIN_TORQUE && speed > MAX_SAFE_SPEED * 4) {
            wreck(16);
        }
    }

    @Override
    protected void tickClient() {
        speed = canTurn() ? Math.min(speed + (speed > MAX_SAFE_SPEED ? 0.3F : 0.1F), maxRenderSpeed()) : Math.max(0, speed - 0.1F);
        if (formed) {
            spin(speed);
        } else {
            phi = 0;
        }
    }

    /** getMaxRenderSpeed: how fast the coil turns, by the shaft speed. */
    private float maxRenderSpeed() {
        int omega = shaft.omega();
        if (omega > MAX_OMEGA) {
            return 512;
        } else if (omega >= 4096) {
            return MAX_SAFE_SPEED;
        } else if (omega >= 2048) {
            return 20F;
        } else if (omega >= 1024) {
            return 7.5F;
        }
        return 4.5F;
    }

    public boolean canTurn() {
        return formed && shaft.power() > 0 && shaft.torque() >= MIN_TORQUE;
    }

    public boolean arePowerRequirementsMet() {
        return shaft.omega() >= MIN_OMEGA && canTurn();
    }

    public float speed() {
        return speed;
    }

    public ShaftInput shaftInput() {
        return shaft;
    }

    /** Wrecks the solenoid and its coil: it blows up, and the coil blocks near it are thrown about. */
    private void wreck(int power) {
        if (ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner())) {
            level.removeBlock(worldPosition, false);
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, power / 4F, true,
                    Level.ExplosionInteraction.BLOCK);
        } else {
            speed = MAX_SAFE_SPEED;
        }
    }

    // ---- the ring ----

    /** addToToroids / removeFromToroids: follow the ring from the magnet 14 east and 2 north, telling each magnet whether it has a solenoid. */
    private void setToroids(boolean has) {
        BlockPos at = worldPosition.offset(14, 0, -2);
        ToroidAim aim = ToroidAim.W;
        int count = 0;
        while (count <= 38) {
            if (level.getBlockEntity(at) instanceof ToroidMagnetBlockEntity magnet) {
                magnet.setHasSolenoid(has);
                aim = magnet.aim();
            } else if (!(level.getBlockEntity(at) instanceof FusionInjectorBlockEntity)) {
                break;
            }
            at = at.offset(aim.xOffset, 0, aim.zOffset);
            count++;
        }
    }

    // ---- structure ----

    @Override
    public MultiStructure structure() {
        return FusionStructures.SOLENOID;
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public void setFormed(boolean formed) {
        if (this.formed && !formed && shaft.omega() > BREAKAGE_OMEGA) {
            this.formed = false;
            wreck(12);
            return;
        }
        if (this.formed != formed) {
            this.formed = formed;
            BlockState state = getBlockState();
            if (level != null && state.hasProperty(SolenoidBlock.FORMED)) {
                level.setBlock(worldPosition, state.setValue(SolenoidBlock.FORMED, formed), 2);
            }
            markForSync();
        }
    }

    @Override
    public boolean onNeutron(net.scwunge.reactorcraft.content.entity.NeutronEntity neutron, Level level, BlockPos pos) {
        return false;
    }

    @Override
    public void setRemoved() {
        if (!checkForToroids && level != null && !level.isClientSide) {
            setToroids(false);
        }
        super.setRemoved();
    }

    public AABB renderBounds() {
        return new AABB(worldPosition).inflate(9, 2, 9);
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putBoolean("Formed", formed);
        shaft.save(tag);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        formed = tag.getBoolean("Formed");
        shaft.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
    }
}
