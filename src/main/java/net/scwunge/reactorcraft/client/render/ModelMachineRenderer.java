package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlockEntity;

import java.util.Set;

/**
 * Draws one of the original Techne models for a machine. Every original renderer used the same transform (move to
 * (0, 2, 1), flip Y and Z, move half a block) and a model's turning group was rotated about the model's vertical axis
 * by minus the machine's spin angle.
 */
public class ModelMachineRenderer<T extends ReactorMachineBlockEntity> implements BlockEntityRenderer<T> {
    private final ModelPart root;
    private final String[] parts;
    private final Set<String> spinning;
    private final ResourceLocation texture;

    /**
     * @param parts    every part of the model, in the original's draw order
     * @param spinning the parts that turn with the machine's spin angle
     */
    public ModelMachineRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer, String texture, String[] parts,
                                String... spinning) {
        this.root = context.bakeLayer(layer);
        this.parts = parts;
        this.spinning = Set.of(spinning);
        this.texture = ReactorCraft.id("textures/entity/" + texture + ".png");
    }

    @Override
    public void render(T machine, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        poseStack.pushPose();
        orientInBlock(machine, poseStack);
        applyBaseTransform(poseStack);
        orient(machine, poseStack);
        renderModel(machine, partialTick, poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        poseStack.popPose();
        renderExtras(machine, partialTick, poseStack, buffers, light, overlay);
    }

    /** glTranslate(0, 2, 1); glScale(1, -1, -1); glTranslate(0.5, 0.5, 0.5). */
    public static void applyBaseTransform(PoseStack poseStack) {
        poseStack.translate(0, 2, 1);
        poseStack.scale(1, -1, -1);
        poseStack.translate(0.5, 0.5, 0.5);
    }

    /** Turns the whole machine about the middle of its block, before the model transform (the gas collector's six facings). */
    protected void orientInBlock(T machine, PoseStack poseStack) {
    }

    /** Turns the whole model, in model space, for machines that face somewhere. */
    protected void orient(T machine, PoseStack poseStack) {
    }

    /** Whether a part is drawn at all (the gas collector's furnace frame). */
    protected boolean shows(T machine, String part) {
        return true;
    }

    /** Drawn in block space after the model: fluids, outlines. */
    protected void renderExtras(T machine, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
    }

    protected void renderModel(T machine, float partialTick, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        for (String name : parts) {
            if (!shows(machine, name)) {
                continue;
            }
            ModelPart part = root.getChild(name);
            if (spinning.contains(name)) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(-machine.phi(partialTick)));
                part.render(poseStack, consumer, light, overlay);
                poseStack.popPose();
            } else {
                part.render(poseStack, consumer, light, overlay);
            }
        }
    }
}
