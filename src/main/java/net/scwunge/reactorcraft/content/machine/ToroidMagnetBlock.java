package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/** A toroid magnet: a RotaryCraft screwdriver turns where it steers plasma, one step a click, backwards while sneaking. */
public class ToroidMagnetBlock extends ReactorMachineBlock {
    private static final ResourceLocation SCREWDRIVER = ResourceLocation.parse("rotarycraft:screwdriver");

    public ToroidMagnetBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type, true, false);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(SCREWDRIVER)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ToroidMagnetBlockEntity magnet) {
                magnet.setAim(player.isShiftKeyDown() ? magnet.aim().previous() : magnet.aim().next());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
