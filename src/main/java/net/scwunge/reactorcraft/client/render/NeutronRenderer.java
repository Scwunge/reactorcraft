package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.scwunge.reactorcraft.ReactorClientConfig;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.NeutronType;
import org.joml.Matrix4f;

/** A neutron is a small square that always faces the camera: dark blue when thermal, light blue when fast. */
public class NeutronRenderer extends EntityRenderer<NeutronEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float HALF = 0.0375F;

    public NeutronRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(NeutronEntity neutron, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (!ReactorClientConfig.VISIBLE_NEUTRONS.get()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, neutron.getBbHeight() / 2, 0);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        int rgb = neutron.neutronSpeed() == NeutronType.NeutronSpeed.FAST ? 0x22AAFF : 0x0000AA;
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        Matrix4f pose = poseStack.last().pose();
        int bright = 0xF000F0;
        vertex(consumer, poseStack, pose, -HALF, -HALF, r, g, b, bright);
        vertex(consumer, poseStack, pose, HALF, -HALF, r, g, b, bright);
        vertex(consumer, poseStack, pose, HALF, HALF, r, g, b, bright);
        vertex(consumer, poseStack, pose, -HALF, HALF, r, g, b, bright);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack, Matrix4f pose, float x, float y, int r, int g, int b, int light) {
        consumer.addVertex(pose, x, y, 0).setColor(r, g, b, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(poseStack.last(), 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(NeutronEntity neutron) {
        return WHITE;
    }
}
