package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.NeutronType;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.core.SteamTile;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.core.WorkingFluid;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorFluids;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Reactor Boiler (TileEntityReactorBoiler): turns water (heavy water too) or ammonia into steam, 200 mB for one unit of steam, as
 * long as it is above 100 C and 50 C over its surroundings; each unit cools it by 5 degrees. Steam goes up into a boiler above
 * or into a steam line on top. Ammonia gas at 650 C blows the boiler and the steam lines above it apart; above 2000 C the
 * boiler itself explodes.
 */
public class ReactorBoilerBlockEntity extends NuclearBoilerBlockEntity implements SteamTile {
    public static final int WATER_PER_STEAM = 200;
    public static final int DETONATION_TEMPERATURE = 650;
    public static final int CAPACITY = 12000;
    public static final int MAX_TEMPERATURE = 2000;

    private WorkingFluid fluid = WorkingFluid.EMPTY;
    private long age;

    public ReactorBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.REACTOR_BOILER.get(), pos, state, CAPACITY);
    }

    @Override
    public ReactorType defaultReactorType() {
        return ReactorType.FISSION;
    }

    @Override
    protected boolean isValidFluid(Fluid fluid) {
        return WorkingFluid.of(fluid) != null;
    }

    public long age() {
        return age;
    }

    @Override
    protected void tickServer() {
        age++;
        super.tickServer();
        if (temperature >= DETONATION_TEMPERATURE && fluid == WorkingFluid.AMMONIA) {
            detonateAmmonia();
            return;
        }
        if (tank.getFluidAmount() >= WATER_PER_STEAM && temperature > 100 && canBoilTankLiquid()) {
            steam++;
            fluid = WorkingFluid.of(tank.getFluid().getFluid());
            removeLiquid(tank, WATER_PER_STEAM);
            temperature -= 5;
            markForSync();
        }
        if (steam <= 0) {
            fluid = WorkingFluid.EMPTY;
        }
        transferSteam();
    }

    private boolean canBoilTankLiquid() {
        if (tank.isEmpty() || WorkingFluid.of(tank.getFluid().getFluid()) == null) {
            return false;
        }
        if (temperature < Thermal.ambient(level, worldPosition) + 50) {
            return false;
        }
        return fluid == WorkingFluid.EMPTY || tank.getFluid().getFluid() == fluid.fluid();
    }

    /** Steam goes up into a boiler above that is empty of steam or holds the same kind. */
    private void transferSteam() {
        if (level.getBlockEntity(worldPosition.above()) instanceof ReactorBoilerBlockEntity above && steam > 0 && fluid != WorkingFluid.EMPTY
                && (above.fluid == WorkingFluid.EMPTY || above.fluid == fluid)) {
            above.fluid = fluid;
            above.steam += steam;
            steam = 0;
        }
    }

    /** Ammonia at 650 C: the steam lines above are blown away, the boiler goes too, and glass nearby shatters. */
    private void detonateAmmonia() {
        if (!ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() || !WorldSafety.griefingAllowed(level)) {
            temperature = DETONATION_TEMPERATURE - 1;
            return;
        }
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition.above());
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            if (!seen.add(at) || !level.getBlockState(at).is(ReactorBlocks.STEAM_LINE.get())) {
                continue;
            }
            if (WorldSafety.mayChange(level, at, owner())) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                puff(at);
            }
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                queue.add(at.relative(dir));
            }
        }
        if (WorldSafety.mayChange(level, worldPosition, owner())) {
            level.setBlockAndUpdate(worldPosition, Blocks.AIR.defaultBlockState());
            puff(worldPosition);
            level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.2F, 1F);
        }
        int r = 8;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-r, -r, -r), worldPosition.offset(r, r, r))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES) || state.is(BlockTags.IMPERMEABLE))
                    && WorldSafety.mayChange(level, pos, owner())) {
                level.destroyBlock(pos.immutable(), true);
            }
        }
    }

    private void puff(BlockPos at) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
    }

    /** A boiler that is hot and in a hot dimension, and holds fluid, bursts (the base class's ambient check). */
    @Override
    protected void afterAmbientStep(int ambient) {
        if (temperature >= 300 && ambient > 100 && !tank.isEmpty() && ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get()
                && WorldSafety.mayChange(level, worldPosition, owner())) {
            level.removeBlock(worldPosition, false);
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 3F, Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    protected void overheat() {
        if (ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner())) {
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 8F, true,
                    Level.ExplosionInteraction.BLOCK);
        } else {
            temperature = MAX_TEMPERATURE;
        }
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    /** onNeutron: heavy water in the tank moderates; fast neutrons in other fluid may be soaked up; then the type's boiler absorption chance. */
    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        NeutronType type = neutron.neutronType();
        if (!tank.isEmpty()) {
            if (tank.getFluid().is(ReactorFluids.HEAVY_WATER.get())) {
                neutron.moderate();
            } else if (neutron.neutronSpeed() == NeutronType.NeutronSpeed.FAST && level.random.nextInt(10) == 0) {
                return true;
            }
            return Chance.of(level.random, type.boilerAbsorptionChance());
        }
        return false;
    }

    @Override
    public int getSteam() {
        return steam;
    }

    public WorkingFluid workingFluid() {
        return fluid;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeClientData(tag, registries);
        tag.putInt("Working", fluid.ordinal());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readClientData(tag, registries);
        fluid = WorkingFluid.byId(tag.getInt("Working"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Working", fluid.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluid = WorkingFluid.byId(tag.getInt("Working"));
    }
}
