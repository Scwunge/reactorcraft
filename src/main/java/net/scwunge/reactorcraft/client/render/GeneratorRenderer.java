package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.client.model.ModelGenerator;
import net.scwunge.reactorcraft.content.machine.ReactorGeneratorBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;

/**
 * The generator (RenderGenerator and ModelGenerator): the whole length of it, a rotor turning inside its housing, from the generator block along to the
 * turbine; drawn once the housing is built. As an item it is a small one.
 */
public class GeneratorRenderer extends ModelMachineRenderer<ReactorGeneratorBlockEntity> {
    private static final String[] ROTOR = {"shape1", "shape1b", "shape1c", "shape3", "shape1d", "shape3a", "shape3b", "shape3c"};
    private static final String[] HOUSING = {"shape2", "shape2a", "shape2b", "shape2c", "shape2d", "shape2e", "shape2f", "shape2g", "shape4", "shape5", "shape7",
            "shape7a", "shape7b", "shape7c"};

    public GeneratorRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer, "generator", ModelGenerator.PARTS);
    }

    @Override
    protected void orient(ReactorGeneratorBlockEntity machine, PoseStack poseStack) {
        if (machine.getLevel() != null) {
            Direction facing = machine.getBlockState().getValue(ReactorMachineBlock.LOOK);
            poseStack.mulPose(Axis.YP.rotationDegrees(FlywheelRenderer.yawOf(facing)));
        }
    }

    @Override
    protected void renderModel(ReactorGeneratorBlockEntity machine, float partialTick, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        boolean inWorld = machine.getLevel() != null;
        if (inWorld && !machine.isFormed()) {
            return;
        }
        float phi = machine.phi(partialTick);
        poseStack.pushPose();
        if (!inWorld) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(0, 0.875, 0.5);
            poseStack.scale(0.125F, 0.125F, 0.125F);
            phi = 0;
        }
        poseStack.translate(0, 1, -7.5);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(phi));
        for (String name : ROTOR) {
            root.getChild(name).render(poseStack, consumer, light, overlay);
        }
        poseStack.popPose();
        for (String name : HOUSING) {
            root.getChild(name).render(poseStack, consumer, light, overlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ReactorGeneratorBlockEntity machine) {
        Direction f = machine.getBlockState().getValue(ReactorMachineBlock.LOOK);
        int l = ReactorGeneratorBlockEntity.LENGTH;
        return new AABB(machine.getBlockPos()).expandTowards(f.getStepX() * l, 0, f.getStepZ() * l).inflate(6);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
