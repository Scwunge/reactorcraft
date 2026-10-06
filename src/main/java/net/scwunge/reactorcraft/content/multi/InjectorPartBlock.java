package net.scwunge.reactorcraft.content.multi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlockEntity;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import org.jetbrains.annotations.Nullable;

/** The casing block that is also a steam injector: it holds lubricant for the turbine, and takes it from buckets and hoses. */
public class InjectorPartBlock extends MultiPartBlock implements EntityBlock {
    public InjectorPartBlock(Properties properties, MultiStructure structure, int variant) {
        super(properties, structure, variant);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorBlockEntities.STEAM_INJECTOR.get().create(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ReactorMachineBlockEntity machine) {
            IFluidHandler tanks = machine.fluidHandler(null);
            if (tanks != null && FluidUtil.getFluidHandler(stack).isPresent() && FluidUtil.interactWithFluidHandler(player, hand, tanks)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
