package net.scwunge.reactorcraft.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.scwunge.reactorcraft.content.entity.RadiationEntity;
import net.scwunge.reactorcraft.registry.ReactorComponents;

import java.util.List;

/**
 * Radiation Cleanup Tool (ItemRadiationCleaner): fill it by right-clicking a water source, then hold right-click to spray the
 * water ahead of you, which washes radiation away (a block of range for each little burst). It uses water and charge (kJ); RotaryCraft has no way to charge
 * it yet, so it starts full of charge but empty of water.
 */
public class RadiationCleanerItem extends Item {
    public static final int CAPACITY = 32000;
    private static final int WATER_PER_TICK = 25;
    private static final int TICK_PER_KJ = 5;

    public RadiationCleanerItem() {
        super(new Item.Properties().stacksTo(1).component(ReactorComponents.CHARGE.get(), CAPACITY).component(ReactorComponents.WATER.get(), 0));
    }

    private static int water(ItemStack stack) {
        return stack.getOrDefault(ReactorComponents.WATER.get(), 0);
    }

    private static int charge(ItemStack stack) {
        return stack.getOrDefault(ReactorComponents.CHARGE.get(), 0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (water(stack) < CAPACITY) {
            BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(Fluids.WATER)
                    && level.getFluidState(hit.getBlockPos()).isSource()) {
                if (!level.isClientSide) {
                    stack.set(ReactorComponents.WATER.get(), Math.min(CAPACITY, water(stack) + 1000));
                    level.setBlockAndUpdate(hit.getBlockPos(), Blocks.AIR.defaultBlockState());
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        if (getUseDuration(stack, player) <= 0) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (charge(stack) <= 0 || water(stack) <= 0) {
            return;
        }
        Vec3 look = user.getLookAngle();
        double d = 0.5 + level.random.nextDouble() * 4;
        Vec3 spot = user.getEyePosition().add(look.scale(d));
        if (!level.isClientSide && remaining % TICK_PER_KJ == 0) {
            for (RadiationEntity radiation : level.getEntitiesOfClass(RadiationEntity.class, new AABB(spot, spot).inflate(1))) {
                radiation.clean();
            }
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.RAIN, spot.x, spot.y, spot.z, 4, 0.05, 0.05, 0.05, 0.1);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (user instanceof Player player && !player.isCreative()) {
            int used = getUseDuration(stack, user) - timeLeft;
            stack.set(ReactorComponents.CHARGE.get(), Math.max(0, charge(stack) - used / TICK_PER_KJ));
            stack.set(ReactorComponents.WATER.get(), Math.max(0, water(stack) - WATER_PER_TICK * used));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return Math.min(72000, Math.min(charge(stack) * TICK_PER_KJ, water(stack) / WATER_PER_TICK));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(String.format("Water: %d/%d mB", water(stack), CAPACITY)));
        tooltip.add(Component.literal("Charge: " + charge(stack) + " kJ"));
    }
}
