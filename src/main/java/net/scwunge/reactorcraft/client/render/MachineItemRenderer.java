package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.HashMap;
import java.util.Map;

/** Draws a modelled machine as an item, by rendering a stand-in block entity of it (the way chests are drawn). */
public final class MachineItemRenderer extends BlockEntityWithoutLevelRenderer implements IClientItemExtensions {
    private final Map<BlockItem, BlockEntity> standIns = new HashMap<>();

    public MachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return this;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof EntityBlock block) {
            BlockEntity standIn = standIns.computeIfAbsent(item, i -> block.newBlockEntity(BlockPos.ZERO, i.getBlock().defaultBlockState()));
            if (standIn != null) {
                Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(standIn, poseStack, buffers, light, overlay);
            }
        }
    }
}
