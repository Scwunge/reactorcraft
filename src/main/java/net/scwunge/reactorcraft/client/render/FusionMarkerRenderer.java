package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.content.machine.FusionMarkerBlockEntity;
import net.scwunge.reactorcraft.core.ToroidAim;
import org.joml.Matrix4f;

import java.util.List;

/**
 * The fusion marker's guide, drawn while it has a redstone signal: the solenoid's footprint in red (two rings of lines three apart) and the ring of magnets
 * in blue, a box for each piece, round the marker, starting 14 blocks south of it.
 */
public class FusionMarkerRenderer implements BlockEntityRenderer<FusionMarkerBlockEntity> {
    /** The solenoid's outline, going round (RenderFusionMarker.renderSolenoid). */
    private static final int[][] OUTLINE = {{8, 3}, {8, -3}, {7, -5}, {6, -6}, {5, -7}, {3, -8}, {-3, -8}, {-5, -7}, {-6, -6}, {-7, -5}, {-8, -3}, {-8, 3},
            {-7, 5}, {-6, 6}, {-5, 7}, {-3, 8}, {3, 8}, {5, 7}, {6, 6}, {7, 5}};

    public FusionMarkerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FusionMarkerBlockEntity marker, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (!marker.renderLines()) {
            return;
        }
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        Matrix4f pose = poseStack.last().pose();
        for (int level : new int[]{1, -2}) {
            for (int i = 0; i < OUTLINE.length; i++) {
                int[] a = OUTLINE[i];
                int[] b = OUTLINE[(i + 1) % OUTLINE.length];
                line(lines, poseStack, pose, a[0], level, a[1], b[0], level, b[1], 255, 0, 0);
            }
        }
        for (int[] p : OUTLINE) {
            line(lines, poseStack, pose, p[0], -2, p[1], p[0], 1, p[1], 255, 0, 0);
        }
        int x = 0;
        int z = 14;
        List<ToroidAim> aims = FusionMarkerBlockEntity.AIM_POINTS;
        for (int i = 0; i < aims.size(); i++) {
            ToroidAim aim = aims.get(i);
            boolean injector = i % 10 == 0;
            box(lines, poseStack, pose, x, z, injector ? 0.0 : 1.0, aim.angle, injector);
            x += aim.xOffset;
            z += aim.zOffset;
        }
        poseStack.popPose();
    }

    /** A box standing for one piece of the ring: a magnet (3 by 3) or, every tenth piece, an injector (3 by 9). */
    private static void box(VertexConsumer lines, PoseStack poseStack, Matrix4f pose, double cx, double cz, double unused, float angle, boolean injector) {
        double rad = Math.toRadians(angle);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double[][] corners = injector ? new double[][]{{-1.5, -2.5}, {1.5, -2.5}, {1.5, 6.5}, {-1.5, 6.5}} : new double[][]{{-1.5, -1.5}, {1.5, -1.5}, {1.5, 1.5}, {-1.5, 1.5}};
        int r = injector ? 0 : 100;
        int g = injector ? 200 : 192;
        int b = 255;
        for (int i = 0; i < 4; i++) {
            double[] p = corners[i];
            double[] q = corners[(i + 1) % 4];
            double px = cx + p[0] * cos + p[1] * sin;
            double pz = cz - p[0] * sin + p[1] * cos;
            double qx = cx + q[0] * cos + q[1] * sin;
            double qz = cz - q[0] * sin + q[1] * cos;
            line(lines, poseStack, pose, px, 1, pz, qx, 1, qz, r, g, b);
            line(lines, poseStack, pose, px, -2, pz, qx, -2, qz, r, g, b);
            line(lines, poseStack, pose, px, -2, pz, px, 1, pz, r, g, b);
        }
    }

    private static void line(VertexConsumer lines, PoseStack poseStack, Matrix4f pose, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) {
            return;
        }
        lines.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, 255).setNormal(poseStack.last(), dx / len, dy / len, dz / len);
        lines.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, 255).setNormal(poseStack.last(), dx / len, dy / len, dz / len);
    }

    @Override
    public AABB getRenderBoundingBox(FusionMarkerBlockEntity marker) {
        return new AABB(marker.getBlockPos()).inflate(30);
    }

    @Override
    public boolean shouldRenderOffScreen(FusionMarkerBlockEntity marker) {
        return true;
    }
}
