package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.client.model.ModelFlywheel;
import net.scwunge.reactorcraft.content.machine.ReactorFlywheelBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;

/**
 * The flywheel (RenderTurbineWheel and ModelFlywheel): three discs, a thin one each side of a thicker, all turning on the shaft, drawn once the casing is
 * built round it. As an item it is a small one.
 */
public class FlywheelRenderer extends ModelMachineRenderer<ReactorFlywheelBlockEntity> {
    private static final double AXIS_HEIGHT = 0.9375;
    private static final String[] RIM = {"shape1", "shape1a"};
    private static final String[] CENTRE = {"shape2", "shape2a", "shape2b", "shape2c", "shape2d", "shape2e", "shape2f", "shape2g", "shape2h", "shape2i", "shape2j"};

    public FlywheelRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer, "flywheel", ModelFlywheel.PARTS);
    }

    @Override
    protected void orient(ReactorFlywheelBlockEntity machine, PoseStack poseStack) {
        if (machine.getLevel() != null) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yawOf(machine.getBlockState().getValue(ReactorMachineBlock.LOOK))));
        }
    }

    /** The original's meta cases: east 270, west 90, south 0, north 180. */
    static float yawOf(Direction facing) {
        return switch (facing) {
            case EAST -> 270;
            case WEST -> 90;
            case NORTH -> 180;
            default -> 0;
        };
    }

    @Override
    protected void renderModel(ReactorFlywheelBlockEntity machine, float partialTick, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        boolean inWorld = machine.getLevel() != null;
        if (inWorld && !machine.isFormed()) {
            return;
        }
        float phi = machine.phi(partialTick);
        poseStack.pushPose();
        if (!inWorld) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(0, 0.75, 0);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            phi = 0;
        }
        poseStack.pushPose();
        poseStack.translate(0, AXIS_HEIGHT, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(phi));
        poseStack.translate(0, -AXIS_HEIGHT, 0);
        poseStack.translate(0, 0, -0.0625);
        poseStack.scale(1, 1, 1.25F);
        draw(RIM, poseStack, consumer, light, overlay);
        centre(poseStack, consumer, light, overlay, 0, -AXIS_HEIGHT, 0.25, 2, 0.35F);
        centre(poseStack, consumer, light, overlay, 0, -AXIS_HEIGHT, -0.25, 2, 0.35F);
        centre(poseStack, consumer, light, overlay, 0, -0.6875, 0, 1.75F, 0.35F);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void centre(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, double x, double y, double z, float scale, float depth) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(scale, scale, depth);
        draw(CENTRE, poseStack, consumer, light, overlay);
        poseStack.popPose();
    }

    private void draw(String[] names, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        for (String name : names) {
            ModelPart part = root.getChild(name);
            part.render(poseStack, consumer, light, overlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(ReactorFlywheelBlockEntity machine) {
        return new AABB(machine.getBlockPos()).inflate(3);
    }
}
