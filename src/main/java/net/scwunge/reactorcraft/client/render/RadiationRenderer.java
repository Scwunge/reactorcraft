package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.entity.RadiationEntity;
import net.scwunge.reactorcraft.registry.ReactorItems;
import org.joml.Matrix4f;

/** Radiation is only visible through radiation goggles: a hazy cloud as big as its range (at most 20 blocks). */
public class RadiationRenderer extends EntityRenderer<RadiationEntity> {
    private static final ResourceLocation TEXTURE = ReactorCraft.id("textures/entity/radiation.png");

    public RadiationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RadiationEntity radiation, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getItemBySlot(EquipmentSlot.HEAD).is(ReactorItems.RADIATION_GOGGLES.get())) {
            return;
        }
        float size = Math.min(20, radiation.range());
        poseStack.pushPose();
        poseStack.translate(0, 0.05, 0);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, 1);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        int bright = 0xF000F0;
        vertex(consumer, poseStack, pose, -0.5F, -0.5F, 0, 1, bright);
        vertex(consumer, poseStack, pose, 0.5F, -0.5F, 1, 1, bright);
        vertex(consumer, poseStack, pose, 0.5F, 0.5F, 1, 0, bright);
        vertex(consumer, poseStack, pose, -0.5F, 0.5F, 0, 0, bright);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack, Matrix4f pose, float x, float y, float u, float v, int light) {
        consumer.addVertex(pose, x, y, 0).setColor(127, 127, 127, 140).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(poseStack.last(), 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(RadiationEntity entity) {
        return TEXTURE;
    }
}
