package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.content.block.SteamBlock;
import net.scwunge.reactorcraft.core.SteamTurbine;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Turbine Core (TileEntityTurbineCore): one stage of a steam turbine. Turbine blocks placed in a line, all facing the same
 * way, form one turbine; the first takes in the steam block that rises under it, and the steam is carried on through the others
 * to the end. The more stages the more of the steam's push becomes shaft torque (1 stage 2.5%, up to 5 for all of it), and the
 * shaft power comes out of the far end of the line, to RotaryCraft machines. It needs lubricant (from a hose at the near end) to
 * spin up, gets slower when anything but air is in the way of its blades, is wrecked a little each time something is caught in
 * them, and spins down when the steam stops.
 */
public class TurbineCoreBlockEntity extends ReactorMachineBlockEntity implements IShaftPowerOutput, SteamTurbine {
    public static final int MAX_SPEED = 65536;
    public static final int MAX_TORQUE = 32768;
    public static final int LUBRICANT_CAPACITY = 64000;
    private static final TagKey<Fluid> LUBRICANT = FluidTags.create(net.minecraft.resources.ResourceLocation.parse("c:lubricant"));

    /** What is in the way of the blades: the most it lets the shaft spin, in rad/s. */
    public enum Interference {
        JAM(0),
        FLUID(512),
        MOB(4096);

        public final int maxSpeed;

        Interference(int maxSpeed) {
            this.maxSpeed = maxSpeed;
        }
    }

    protected int steam;
    protected int omega;
    private boolean ammonia;
    private int damage;
    private boolean enabled = true;
    private int stage;
    @Nullable
    private Interference interference;
    private int forcedLubricant;
    private int lubeTicks;

    private final FluidTank lubricant;
    private final IFluidHandler lubricantIn;

    public TurbineCoreBlockEntity(BlockPos pos, BlockState state) {
        this(ReactorBlockEntities.TURBINE_CORE.get(), pos, state);
    }

    protected TurbineCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        lubricant = addTank("Lubricant", LUBRICANT_CAPACITY, s -> s.getFluid().is(LUBRICANT));
        lubricantIn = FluidAccess.fillOnly(lubricant);
    }

    // ---- the line of turbines ----

    /** The way steam moves through this turbine, and shaft power leaves. */
    @Override
    public Direction steamMovement() {
        Direction look = getBlockState().getValue(ReactorMachineBlock.LOOK);
        return look.getAxis().isHorizontal() ? look : Direction.SOUTH;
    }

    @Nullable
    private TurbineCoreBlockEntity neighbour(Direction dir) {
        if (level.getBlockEntity(worldPosition.relative(dir)) instanceof TurbineCoreBlockEntity other && other.steamMovement() == steamMovement()) {
            return other;
        }
        return null;
    }

    /** The turbine block that passes its steam to this one. */
    @Nullable
    private TurbineCoreBlockEntity behind() {
        return neighbour(steamMovement().getOpposite());
    }

    /** The turbine block this one passes its steam to. */
    @Nullable
    private TurbineCoreBlockEntity ahead() {
        return neighbour(steamMovement());
    }

    protected int maxStage() {
        return 4;
    }

    /** calcStage: how many turbine blocks are behind this one (up to the maximum). */
    private int calcStage() {
        TurbineCoreBlockEntity behind = behind();
        if (behind == null) {
            return 0;
        }
        int s = behind.calcStage();
        return s >= maxStage() ? maxStage() : s + 1;
    }

    @Override
    public int stage() {
        return stage;
    }

    /** getNumberStagesTotal: the length of the whole line this block is in. */
    @Override
    public int totalStages() {
        TurbineCoreBlockEntity ahead = ahead();
        return ahead != null ? ahead.totalStages() : calcStage() + 1;
    }

    // ---- ticking ----

    @Override
    protected void tickServer() {
        boolean thermal = thermalStep();
        int before = omega + steam * 1000 + damage;
        stage = calcStage();
        distributeLubricant();
        readSurroundings();
        followHead();
        enviroTest();
        if (steam > 0) {
            if (thermal) {
                steam -= consumedSteam();
            }
        }
        if (omega == 0) {
            steam = 0;
        } else {
            lubeTicks++;
            if (!lubricant.isEmpty() && lubeTicks >= 20) {
                lubeTicks = 0;
                removeLiquid(lubricant, consumedLubricant());
            }
        }
        steam = (int) (steam * damageEfficiency());
        if (before != omega + steam * 1000 + damage) {
            markForSync();
        }
    }

    @Override
    protected void tickClient() {
        if (omega > 0) {
            spin((float) (0.2 * Math.pow(Math.log(omega + 1) / Math.log(2), 1.05)));
        }
    }

    protected int consumedLubricant() {
        return 20;
    }

    private int consumedSteam() {
        return steam / 32 + 1;
    }

    private float damageEfficiency() {
        return damage > 0 ? 1F / (damage + 1) : 1;
    }

    /** distributeLubricant: lubricant flows along the line, half of any difference at a time. */
    private void distributeLubricant() {
        TurbineCoreBlockEntity behind = behind();
        if (behind != null) {
            int max = Math.min(lubricant.getCapacity() - lubricant.getFluidAmount(), 1000);
            int dl = behind.lubricant.getFluidAmount() - lubricant.getFluidAmount();
            if (dl > 1 && !behind.lubricant.isEmpty()) {
                int amount = Math.min(dl / 2, max);
                addLiquid(lubricant, fluidOf(behind.lubricant), amount);
                removeLiquid(behind.lubricant, amount);
            }
        }
    }

    /** readSurroundings: anything solid in the way of the blades jams the shaft; liquid slows it. The first stage also takes in steam. */
    private void readSurroundings() {
        interference = null;
        Direction axis = steamMovement();
        int r = 3;
        double radius = 1.5 + stage / 2;
        for (int a = -r; a <= r; a++) {
            for (int b = -r; b <= r; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos at = axis.getAxis() == Direction.Axis.Z ? worldPosition.offset(a, b, 0) : worldPosition.offset(0, b, a);
                if (Math.sqrt(a * a + b * b) > radius) {
                    continue;
                }
                BlockState state = level.getBlockState(at);
                if (!isSoft(state)) {
                    omega = 0;
                    if (interference == null || interference.maxSpeed > Interference.JAM.maxSpeed) {
                        interference = Interference.JAM;
                    }
                } else if (!state.getFluidState().isEmpty()) {
                    if (interference == null || interference.maxSpeed > Interference.FLUID.maxSpeed) {
                        interference = Interference.FLUID;
                    }
                }
            }
        }
        if (stage == 0) {
            boolean accelerate = enabled && intakeSteam();
            updateSpeed(accelerate);
        }
    }

    private static boolean isSoft(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty();
    }

    /** intakeSteam: powered steam in the block under the first stage gives it steam (twice as much if it is ammonia). Returns whether to accelerate. */
    protected boolean intakeSteam() {
        BlockState below = level.getBlockState(worldPosition.below());
        if (stage == 0 && below.is(ReactorBlocks.STEAM.get()) && below.getValue(SteamBlock.POWERED) && !below.getValue(SteamBlock.MOVED)) {
            if (below.getValue(SteamBlock.AMMONIA)) {
                steam += 2;
                ammonia = true;
            } else {
                steam++;
                ammonia = false;
            }
            return true;
        }
        return false;
    }

    /** updateSpeed: without lubricant the shaft does not speed up; it gains about 64 rad/s a tick, and loses a 256th of its speed otherwise. */
    private void updateSpeed(boolean up) {
        if (lubricant.isEmpty()) {
            up = false;
        }
        if (up) {
            int max = maxSpeed();
            if (omega < max) {
                omega += 4 * (int) (Math.log(max + 1D) / Math.log(2));
                omega = Math.min(omega, max);
            }
        } else if (omega > 0) {
            omega -= omega / 256 + 1;
        }
    }

    public int maxSpeed() {
        return MAX_SPEED;
    }

    public int maxTorque() {
        return MAX_TORQUE;
    }

    /** followHead: takes the speed and steam of the block behind, and tells it about anything in the way of the blades here. */
    private void followHead() {
        TurbineCoreBlockEntity behind = behind();
        if (behind != null) {
            omega = behind.omega;
            steam = behind.steam;
            ammonia = behind.ammonia;
        }
        TurbineCoreBlockEntity ahead = ahead();
        if (ahead != null && ahead.interference != null) {
            interference = ahead.interference;
        }
    }

    /** enviroTest: living things in the blades are hit, wreck them a little, and slow the shaft to 4096 rad/s. */
    private void enviroTest() {
        double r = 2 + stage / 2;
        AABB box = boundingBox();
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity e : hit) {
            if (omega > 0 && e.position().distanceTo(worldPosition.getCenter()) < r && canDamageTurbine(e)) {
                level.explode(null, e.getX(), e.getY() + e.getEyeHeight(), e.getZ(), 2F, Level.ExplosionInteraction.NONE);
                e.hurt(level.damageSources().generic(), 2);
                breakTurbine();
                e.setDeltaMovement(e.getDeltaMovement().add(0.4 * (e.getX() - worldPosition.getX() - 0.4) + level.random.nextDouble() * 0.1,
                        0.4 * (e.getY() - worldPosition.getY() - 0.4), 0.4 * (e.getZ() - worldPosition.getZ() - 0.4) + level.random.nextDouble() * 0.1));
                e.hurtMarked = true;
                if (interference == null || interference.maxSpeed > Interference.MOB.maxSpeed) {
                    interference = Interference.MOB;
                }
            }
        }
        if (interference != null) {
            omega = Math.min(omega, interference.maxSpeed);
        }
    }

    private AABB boundingBox() {
        int r = 2 + stage;
        AABB box = new AABB(worldPosition);
        return steamMovement().getAxis() == Direction.Axis.Z ? box.inflate(r / 2, r / 2, 0) : box.inflate(0, r / 2, r / 2);
    }

    public static boolean canDamageTurbine(LivingEntity e) {
        return !(e instanceof Player player) || !player.isCreative() && !player.isSpectator();
    }

    protected void breakTurbine() {
        damage++;
    }

    // ---- shaft power ----

    private double efficiency() {
        return switch (totalStages()) {
            case 1 -> 0.025;
            case 2 -> 0.1;
            case 3 -> 0.25;
            case 4 -> 0.5;
            case 5 -> 1;
            default -> 0;
        };
    }

    protected float torqueFactor() {
        return 1;
    }

    /** getGenTorque: steam gives 24 Nm of torque a unit; when there is none, what it needs to keep turning. */
    public final int generatedTorque() {
        int torque = steam > 0 ? (int) (steam * 24 * torqueFactor()) : omega / 16 + 1;
        int out = omega > 0 ? (int) (torque * efficiency()) : 0;
        return Math.min(out, maxTorque());
    }

    public final long generatedPower() {
        return (long) generatedTorque() * omega;
    }

    public final int omega() {
        return omega;
    }

    @Override
    public int getTorqueOut(Direction side) {
        return side == steamMovement() && generatedPower() > 0 ? generatedTorque() : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return side == steamMovement() && generatedPower() > 0 ? omega : 0;
    }

    // ---- state ----

    public int damage() {
        return damage;
    }

    public void repair() {
        if (damage > 0) {
            damage--;
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

    public boolean isAmmonia() {
        return ammonia;
    }

    @Nullable
    public Interference interference() {
        return interference;
    }

    public int steam() {
        return steam;
    }

    public FluidTank lubricantTank() {
        return lubricant;
    }

    /** Lubricant goes in from the hose at the near end. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side == steamMovement().getOpposite() ? lubricantIn : null;
    }

    /** When the block is broken, its neighbours in the line keep its lubricant (the original's forcedlube). */
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && !lubricant.isEmpty()) {
            for (Direction dir : new Direction[]{steamMovement(), steamMovement().getOpposite()}) {
                TurbineCoreBlockEntity other = neighbour(dir);
                if (other != null) {
                    addLiquid(other.lubricant, fluidOf(lubricant), Math.min(lubricant.getFluidAmount(),
                            other.lubricant.getCapacity() - other.lubricant.getFluidAmount()));
                    break;
                }
            }
        }
        super.setRemoved();
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Omega", omega);
        tag.putInt("Steam", steam);
        tag.putInt("Stage", stage);
        tag.putInt("Damage", damage);
        tag.putBoolean("Ammonia", ammonia);
        tag.putBoolean("Enabled", enabled);
        tag.putInt("Interference", interference == null ? -1 : interference.ordinal());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        omega = tag.getInt("Omega");
        steam = tag.getInt("Steam");
        stage = tag.getInt("Stage");
        damage = tag.getInt("Damage");
        ammonia = tag.getBoolean("Ammonia");
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        int i = tag.getInt("Interference");
        interference = i >= 0 && i < Interference.values().length ? Interference.values()[i] : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Omega", omega);
        tag.putInt("Damage", damage);
        tag.putBoolean("Enabled", enabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        omega = tag.getInt("Omega");
        damage = tag.getInt("Damage");
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    }
}
