package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** A turbine block: right-clicking it with a RotaryCraft screwdriver turns the way the steam flows through it. */
public class TurbineCoreBlock extends ReactorMachineBlock {
    private static final ResourceLocation SCREWDRIVER = ResourceLocation.parse("rotarycraft:screwdriver");

    public TurbineCoreBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type, true, true);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(SCREWDRIVER)) {
            if (!level.isClientSide) {
                BlockState turned = turnedBy(state, level, pos, hit);
                if (turned != state) {
                    level.setBlock(pos, turned, 3);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** What the screwdriver does to the block: turns it a quarter. */
    protected BlockState turnedBy(BlockState state, Level level, BlockPos pos, BlockHitResult hit) {
        return state.setValue(LOOK, state.getValue(LOOK).getClockWise());
    }
}
