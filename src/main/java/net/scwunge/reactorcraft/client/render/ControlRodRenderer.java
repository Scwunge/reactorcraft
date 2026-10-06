package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.client.model.ModelControl;
import net.scwunge.reactorcraft.content.machine.ControlRodBlockEntity;

/** The Control Rod: the rod parts of the model (shape 6 and up) slide up and out of the core by a 28th of a block per step. */
public class ControlRodRenderer extends ModelMachineRenderer<ControlRodBlockEntity> {
    private final net.minecraft.client.model.geom.ModelPart rootPart;

    public ControlRodRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer, "control_rod", ModelControl.PARTS);
        this.rootPart = context.bakeLayer(layer);
    }

    private static boolean isRodPart(String name) {
        return name.startsWith("shape6") || name.startsWith("shape7") || name.startsWith("shape8") || name.startsWith("shape9");
    }

    @Override
    protected void renderModel(ControlRodBlockEntity machine, float partialTick, PoseStack poseStack, VertexConsumer consumer, int light,
                               int overlay) {
        double lift = -machine.rodPosition() / 28D;
        for (String name : ModelControl.PARTS) {
            ModelPart part = rootPart.getChild(name);
            if (isRodPart(name)) {
                poseStack.pushPose();
                poseStack.translate(0, lift, 0);
                part.render(poseStack, consumer, light, overlay);
                poseStack.popPose();
            } else {
                part.render(poseStack, consumer, light, overlay);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(ControlRodBlockEntity machine) {
        return new AABB(machine.getBlockPos()).expandTowards(0, 2, 0);
    }
}
