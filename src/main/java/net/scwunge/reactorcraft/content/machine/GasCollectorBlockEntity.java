package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.RefrigeratorAttachment;
import org.jetbrains.annotations.Nullable;

/**
 * Gas Collector (TileEntityGasCollector): sits against a lit furnace burning coal or wood and collects 10 mB of carbon
 * dioxide a tick from its smoke, into a 1000 mB tank that drains out of the back. The side it faces is the side it reads.
 */
public class GasCollectorBlockEntity extends ReactorMachineBlockEntity implements RefrigeratorAttachment {
    public static final int CAPACITY = 1000;
    public static final int CO2_PER_TICK = 10;

    /** Animation: counts down from 512 after a refrigerator cycle. */
    public int ticks = 512;

    private final FluidTank tank;
    private final IFluidHandler output;

    public GasCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.GAS_COLLECTOR.get(), pos, state, 0);
        tank = addTank("co2collector", CAPACITY, s -> false);
        output = FluidAccess.drainOnly(tank);
    }

    /** The side it reads from: the way it was looking when placed. */
    public Direction readDirection() {
        return getBlockState().getValue(ReactorMachineBlock.LOOK);
    }

    @Override
    protected void tickServer() {
        BlockPos at = worldPosition.relative(readDirection());
        BlockState state = level.getBlockState(at);
        if (state.is(Blocks.FURNACE) && state.getValue(AbstractFurnaceBlock.LIT)
                && level.getBlockEntity(at) instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack fuel = furnace.getItem(1);
            if (!fuel.isEmpty() && isSmokyFuel(fuel)) {
                addLiquid(tank, ReactorFluids.CO2.get(), CO2_PER_TICK);
            }
        }
    }

    /** ItemMaterial.COAL or WOOD. */
    private static boolean isSmokyFuel(ItemStack fuel) {
        return fuel.is(ItemTags.COALS) || fuel.is(ItemTags.LOGS_THAT_BURN) || fuel.is(ItemTags.PLANKS)
                || fuel.is(ItemTags.WOODEN_SLABS) || fuel.is(ItemTags.WOODEN_STAIRS) || fuel.is(ItemTags.SAPLINGS)
                || fuel.is(Items.STICK) || fuel.is(Items.COAL_BLOCK);
    }

    @Override
    protected void tickClient() {
        if (ticks > 0) {
            ticks -= 8;
        }
    }

    /** onCompleteCycle: a refrigerator cycle beside it adds liquid oxygen. */
    @Override
    public void onCompleteCycle(int liquidNitrogen) {
        addLiquid(tank, ReactorFluids.LIQUID_OXYGEN.get(), liquidNitrogen * 2 / 7);
    }

    public boolean hasFurnace() {
        BlockState state = level.getBlockState(worldPosition.relative(readDirection()));
        return state.is(Blocks.FURNACE);
    }

    /** Fluid comes out of the side opposite the one it reads. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side == readDirection().getOpposite() ? output : null;
    }

    public FluidTank tank() {
        return tank;
    }
}
