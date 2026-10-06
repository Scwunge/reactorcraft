package net.scwunge.reactorcraft.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scwunge.reactorcraft.content.machine.CpuBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMenu;
import net.scwunge.reactorcraft.core.Linkable;
import net.scwunge.reactorcraft.registry.ReactorComponents;

import java.util.List;

/**
 * Remote Control (ItemRemoteControl): link it to a Central Control block by using it on the block, and then right-click in the
 * air to open the CPU's screen from a distance. Using it on a reactor core while linked makes the CPU watch that core's
 * temperature. It reaches 4 blocks for each doubling of its charge (so 56 blocks full) and uses one charge a use. Unlike the original it
 * only works in the CPU's own dimension, and RotaryCraft has no way to charge it yet, so it starts full.
 */
public class RemoteControlItem extends Item {
    public static final int FULL_CHARGE = 32000;

    public RemoteControlItem() {
        super(new Item.Properties().stacksTo(1).component(ReactorComponents.CHARGE.get(), FULL_CHARGE));
    }

    public static int charge(ItemStack stack) {
        return stack.getOrDefault(ReactorComponents.CHARGE.get(), 0);
    }

    public static int range(ItemStack stack) {
        int charge = charge(stack);
        return charge <= 0 ? 0 : 4 * (int) (Math.log(charge) / Math.log(2));
    }

    /** Whether this remote, held by {@code player}, can work the CPU at {@code cpu}: linked to it, charged, close enough, same world. */
    public static boolean canReach(ItemStack stack, Player player, BlockPos cpu) {
        GlobalPos linked = stack.get(ReactorComponents.LINKED_CPU.get());
        return stack.getItem() instanceof RemoteControlItem && linked != null && linked.pos().equals(cpu)
                && linked.dimension() == player.level().dimension() && charge(stack) > 0
                && player.blockPosition().distSqr(cpu) <= (range(stack) + 0.5) * (range(stack) + 0.5);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (be instanceof CpuBlockEntity) {
            if (!level.isClientSide) {
                stack.set(ReactorComponents.LINKED_CPU.get(), GlobalPos.of(level.dimension(), context.getClickedPos()));
                if (player != null) {
                    player.displayClientMessage(Component.literal("Linked to Central Control at " + context.getClickedPos().toShortString()), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        GlobalPos linked = stack.get(ReactorComponents.LINKED_CPU.get());
        if (be instanceof Linkable core && linked != null && linked.dimension() == level.dimension()) {
            if (!level.isClientSide && level.getBlockEntity(linked.pos()) instanceof CpuBlockEntity cpu) {
                cpu.addTemperatureCheck(core, context.getClickedPos());
                if (player != null) {
                    player.displayClientMessage(Component.literal("Linking " + be.getBlockState().getBlock().getName().getString() + " to the Central Control"), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GlobalPos linked = stack.get(ReactorComponents.LINKED_CPU.get());
        if (linked != null && canReach(stack, player, linked.pos())) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel
                    && level.getBlockEntity(linked.pos()) instanceof CpuBlockEntity cpu) {
                stack.set(ReactorComponents.CHARGE.get(), charge(stack) - 1);
                ReactorMenu.open(serverPlayer, cpu);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos linked = stack.get(ReactorComponents.LINKED_CPU.get());
        tooltip.add(Component.literal(linked == null ? "No linked CPU"
                : "Linked to CPU in " + linked.dimension().location() + " at " + linked.pos().toShortString()));
        tooltip.add(Component.literal("Charge: " + charge(stack) + " kJ"));
    }
}
