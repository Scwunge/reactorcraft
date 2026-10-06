package net.scwunge.reactorcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base for every ReactorCraft machine (the original's TileEntityReactorBase): a temperature that settles towards the
 * surroundings and is exchanged with neighbouring reactor parts once a second, the player who placed it (so world changes
 * are made in their name and claims apply), and a small client sync.
 */
public abstract class ReactorBlockEntity extends BlockEntity {
    protected static final Direction[] DIRS = Direction.values();

    protected int temperature;
    private int thermalTicks;
    @Nullable
    private UUID owner;
    private boolean firstTick = true;
    private boolean temperatureKnown;

    protected ReactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static <T extends ReactorBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, T entity) {
        ReactorBlockEntity be = entity;
        if (be.firstTick) {
            be.firstTick = false;
            if (!be.temperatureKnown) {
                be.temperature = Thermal.ambient(level, pos);
                be.temperatureKnown = true;
            }
            be.onFirstTick();
        }
        be.tickServer();
    }

    public static <T extends ReactorBlockEntity> void clientTick(Level level, BlockPos pos, BlockState state, T be) {
        be.tickClient();
    }

    protected void onFirstTick() {
    }

    protected abstract void tickServer();

    protected void tickClient() {
    }

    /** True once a second (the original's 20-tick thermal StepTimer). */
    protected boolean thermalStep() {
        if (++thermalTicks >= 20) {
            thermalTicks = 0;
            return true;
        }
        return false;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    /** The reactor type this part belongs to, or null if it isn't a reactor part. */
    @Nullable
    public ReactorType reactorType() {
        return this instanceof ReactorTyped typed ? typed.getReactorType() : null;
    }

    // ---- heat (TileEntityReactorBase.updateTemperature) ----

    /** Settles towards the surroundings, heats the world around it, and takes heat from hotter neighbouring parts. */
    protected void updateTemperature() {
        Level level = getLevel();
        BlockPos pos = getBlockPos();
        int ambient = Thermal.reactorAmbient(level, pos);
        int dT = ambient - temperature;
        if (dT != 0) {
            int d = Thermal.isExposedToAir(level, pos) ? 32 : 64;
            int diff = 1 + dT / d;
            if (diff <= 1) {
                diff = dT / Math.abs(dT);
            }
            temperature += diff;
        }
        Thermal.heatEnvironment(level, pos, Math.min(temperature, 1000), owner);
        afterAmbientStep(ambient);
        for (Direction dir : DIRS) {
            BlockPos at = pos.relative(dir);
            if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof ReactorBlockEntity other) || !(other instanceof Temperatured tr)) {
                continue;
            }
            int localAmbient = Thermal.ambient(level, at);
            int t = tr.getTemperature();
            int delta = (t - temperature) - Math.max(0, ambient - localAmbient);
            delta = (int) (delta * other.heatThroughputTo(this));
            if (delta > 0) {
                int fraction = heatFractionFrom(other);
                int newT = t - delta / fraction;
                double add = delta / fraction * (double) other.heatEfficiencyTo(this);
                temperature += add;
                tr.setTemperature(newT);
                onHeatReceived(other, add);
            }
        }
        setChanged();
    }

    /** Hook after settling towards ambient, before neighbour exchange (the reactor boiler checks for a steam explosion here). */
    protected void afterAmbientStep(int ambient) {
    }

    /** Called when this part has taken {@code amount} degrees from {@code from}. */
    protected void onHeatReceived(ReactorBlockEntity from, double amount) {
    }

    private int heatFractionFrom(ReactorBlockEntity other) {
        return other instanceof TemperaturedReactorTyped typed ? heatConductionFraction(typed) : 4;
    }

    /** What share of the difference flows from this part into {@code receiver}. */
    float heatThroughputTo(ReactorBlockEntity receiver) {
        return receiver instanceof TemperaturedReactorTyped typed ? heatConductionThroughput(typed) : 1;
    }

    float heatEfficiencyTo(ReactorBlockEntity receiver) {
        return receiver instanceof TemperaturedReactorTyped typed ? heatConductionEfficiency(typed) : 0;
    }

    /** For heat flowing FROM {@code other} into this part: divide the difference by this. */
    protected int heatConductionFraction(TemperaturedReactorTyped other) {
        return 4;
    }

    /** For heat flowing from this part TO {@code other}. */
    protected float heatConductionThroughput(TemperaturedReactorTyped other) {
        return 1;
    }

    /** For heat flowing from this part TO {@code other}: how much of what leaves arrives. */
    protected float heatConductionEfficiency(TemperaturedReactorTyped other) {
        if (other instanceof ReactorBlockEntity be && be.isControlOrCpu()) {
            return controlCpuHeatEfficiency();
        }
        ReactorType mine = reactorType();
        ReactorType theirs = other.getReactorType();
        if (mine == theirs) {
            return 1;
        }
        if (mine == null || theirs == null) {
            return 0;
        }
        return mine.typeMismatchHeatEfficiency();
    }

    /** Control rods and the CPU take heat from every reactor type. */
    protected boolean isControlOrCpu() {
        return false;
    }

    protected float controlCpuHeatEfficiency() {
        return 1;
    }

    public int temperature() {
        return temperature;
    }

    // ---- saving and sync ----

    protected void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** State the client needs (renderer, GUI header). Sent with {@link #syncToClient}. */
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Temp", temperature);
    }

    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        temperature = tag.getInt("Temp");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeClientData(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientData(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Update packets carry {@link #writeClientData} only, so read them the same way (the default treats them as saves). */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readClientData(packet.getTag(), registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Temp", temperature);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.getInt("Temp");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        temperatureKnown = tag.contains("Temp");
    }
}
