package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.scwunge.reactorcraft.content.entity.PlasmaEntity;
import org.joml.Matrix4f;

/** Plasma is a bright pink-white square that always faces the camera, a little larger than a neutron. */
public class PlasmaRenderer extends EntityRenderer<PlasmaEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float HALF = 0.2F;

    public PlasmaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PlasmaEntity plasma, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(0, plasma.getBbHeight() / 2, 0);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        Matrix4f pose = poseStack.last().pose();
        vertex(consumer, poseStack, pose, -HALF, -HALF);
        vertex(consumer, poseStack, pose, HALF, -HALF);
        vertex(consumer, poseStack, pose, HALF, HALF);
        vertex(consumer, poseStack, pose, -HALF, HALF);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack, Matrix4f pose, float x, float y) {
        consumer.addVertex(pose, x, y, 0).setColor(255, 140, 255, 190).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(poseStack.last(), 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(PlasmaEntity plasma) {
        return WHITE;
    }
}
