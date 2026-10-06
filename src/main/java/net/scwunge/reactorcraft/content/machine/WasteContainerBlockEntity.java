package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.ReactorConfig;
import net.scwunge.reactorcraft.content.waste.Isotope;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.core.RadiationHooks;
import net.scwunge.reactorcraft.core.Thermal;
import net.scwunge.reactorcraft.core.WorldSafety;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Waste Container (TileEntityWasteContainer): holds the short-lived waste (half-life under six years) in a 9x3 grid
 * while it decays, a stack of one per slot. It leaks radiation, heats up with the waste in it (about 3.2 degrees a
 * second per item), and is cooled by water source blocks beside it, boiling them away above 100 C. Above 600 C it
 * melts down. Nothing can be taken out by automation unless it is long-lived, which the container never holds.
 */
public class WasteContainerBlockEntity extends WasteUnitBlockEntity implements NeutronTile {
    public static final int WIDTH = 9;
    public static final int HEIGHT = 3;
    public static final int MAX_TEMPERATURE = 600;
    /** ReikaNuclearHelper.getWasteDecayHeat: Avogadro's number of half-eV decays warming 15000 units of 1 C/J. */
    public static final double WASTE_DECAY_HEAT = 6.02e23 * (0.5 * (1.602 / 1e19)) / 15000;
    /** ReikaThermoHelper.WATER_BLOCK_HEAT / (15000 * 1). */
    private static final double WATER_COOLING = 4.18e6 / 15000;

    public WasteContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.WASTE_CONTAINER.get(), pos, state, WIDTH * HEIGHT);
    }

    @Override
    protected void tickServer() {
        if (thermalStep()) {
            temperature = (int) (temperature + countWaste() * WASTE_DECAY_HEAT);
            distributeHeat();
        }
        decayWaste();
        feed();
    }

    private void distributeHeat() {
        int ambient = Thermal.ambient(level, worldPosition);
        if (temperature > ambient) {
            for (Direction dir : Direction.values()) {
                BlockPos at = worldPosition.relative(dir);
                if (level.getFluidState(at).is(net.minecraft.world.level.material.Fluids.WATER) && level.getFluidState(at).isSource()) {
                    temperature = (int) (temperature - WATER_COOLING);
                    if (WorldSafety.mayChange(level, at, owner())) {
                        level.setBlockAndUpdate(at, temperature > 100 ? Blocks.AIR.defaultBlockState()
                                : Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 6));
                    }
                    break;
                }
            }
        }
        if (temperature < ambient) {
            temperature = ambient;
        }
        if (temperature > MAX_TEMPERATURE) {
            onMeltdown();
        } else if (temperature > MAX_TEMPERATURE / 2 && level.random.nextInt(6) == 0) {
            smoke();
        } else if (temperature > MAX_TEMPERATURE / 4 && level.random.nextInt(20) == 0) {
            smoke();
        }
        setChanged();
    }

    private void smoke() {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + level.random.nextDouble(), worldPosition.getY() + 1,
                    worldPosition.getZ() + level.random.nextDouble(), 1, 0, 0, 0, 0);
        }
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
    }

    /** onMeltdown: a flaming explosion of power 9 (if meltdowns may destroy blocks here) and lethal contamination. */
    private void onMeltdown() {
        boolean exploded = false;
        if (ReactorConfig.MELTDOWNS_DESTROY_BLOCKS.get() && WorldSafety.mayChange(level, worldPosition, owner())) {
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 9F, true,
                    Level.ExplosionInteraction.BLOCK);
            exploded = true;
        }
        RadiationHooks.contaminateArea(level, worldPosition);
        if (!exploded) {
            temperature = MAX_TEMPERATURE;
        }
    }

    @Override
    protected void collapseInventory() {
        for (int i = 0; i < size(); i++) {
            for (int k = size() - 1; k > 0; k--) {
                if (stack(k).isEmpty()) {
                    setStack(k, stack(k - 1));
                    setStack(k - 1, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public boolean leaksRadiation() {
        return true;
    }

    @Override
    public boolean isValidIsotope(Isotope isotope) {
        return !isotope.isLongLived();
    }

    @Override
    protected double baseDecayRate() {
        return 1;
    }

    @Override
    protected boolean accountForOutGameTime() {
        return false;
    }

    @Override
    public boolean canExtractFromSide(int slot, @Nullable Direction side) {
        return WasteManager.isLongLived(stack(slot));
    }

    /** Neutrons pass straight through a waste container. */
    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        return false;
    }

    public int maxTemperature() {
        return MAX_TEMPERATURE;
    }

    // ---- GUI ----

    @Override
    public void addMenuSlots(ReactorMenu menu) {
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                menu.addMachineSlot(row * WIDTH + col, 8 + col * 18, 22 + row * 18);
            }
        }
    }

    @Override
    public int inventoryY() {
        return 93;
    }

    @Override
    protected int guiValueCount() {
        return 1;
    }

    @Override
    protected int getGuiValue(int index) {
        return temperature;
    }
}
