package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;
import net.scwunge.reactorcraft.content.machine.TurbineCoreBlockEntity;

/**
 * Draws a turbine block (RenderTurbine and ModelTurbine): a shaft with rings of blades turning on it, two rings a block, every
 * block of the turbine bigger than the one before. Wear thins the blades. The shape of the blades comes from the stage, so
 * there is one little model per stage.
 */
public class TurbineRenderer implements BlockEntityRenderer<TurbineCoreBlockEntity> {
    /** The most stages a turbine can have. */
    public static final int STAGES = 5;
    private static final double AXIS_HEIGHT = 0.9375;
    private static final double RING_SPACING = 0.25;

    private final ModelPart[] models = new ModelPart[STAGES];
    private final ResourceLocation texture;

    public TurbineRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation[] layers, String texture) {
        for (int i = 0; i < STAGES; i++) {
            models[i] = context.bakeLayer(layers[i]);
        }
        this.texture = ReactorCraft.id("textures/entity/" + texture + ".png");
    }

    // ---- model data from ModelTurbine ----

    private static int bladeLength(int stage) {
        return switch (stage) {
            case 0 -> 16;
            case 1 -> 24;
            case 2 -> 28;
            case 3 -> 33;
            case 4 -> 40;
            default -> 4;
        };
    }

    private static int bladeWidth(int stage) {
        return switch (stage) {
            case 3 -> 3;
            case 4 -> 4;
            case 5 -> 6;
            case 6 -> 8;
            default -> 2;
        };
    }

    private static int angularSeparation(int stage) {
        return switch (stage) {
            case 0 -> 8;
            case 1 -> 5;
            case 5 -> 9;
            case 6 -> 10;
            case 2, 3, 4 -> 8;
            default -> 10;
        };
    }

    private static int bladeTwist(int stage) {
        return switch (stage) {
            case 0 -> 10;
            case 1 -> 15;
            case 2 -> 20;
            case 3 -> 30;
            case 4, 5 -> 45;
            case 6 -> 50;
            default -> 10;
        };
    }

    private static double scaleFactor(int stage) {
        return stage < 1 ? 1.3 : 1.1;
    }

    /** One stage's model: the two shaft pieces and a blade sized for the stage. */
    public static LayerDefinition createLayer(int stage) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("shaft1", CubeListBuilder.create().texOffs(0, 106).mirror().addBox(-2F, -2F, 0F, 4F, 4F, 16F),
                PartPose.offsetAndRotation(0F, 15F, -8F, 0F, 0F, 0.7853982F));
        root.addOrReplaceChild("shaft1a", CubeListBuilder.create().texOffs(0, 106).mirror().addBox(-2F, -2F, 0F, 4F, 4F, 16F),
                PartPose.offset(0F, 15F, -8F));
        root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0).mirror()
                        .addBox(-0.5F, -bladeLength(stage), -bladeWidth(stage) / 2F, 1F, bladeLength(stage), bladeWidth(stage)),
                PartPose.offset(0F, 15F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    // ---- drawing ----

    @Override
    public void render(TurbineCoreBlockEntity turbine, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        poseStack.pushPose();
        ModelMachineRenderer.applyBaseTransform(poseStack);
        boolean inWorld = turbine.getLevel() != null;
        Direction facing = turbine.getBlockState().getValue(ReactorMachineBlock.LOOK);
        poseStack.mulPose(Axis.YP.rotationDegrees(switch (facing) {
            case WEST -> 90;
            case EAST -> 270;
            case NORTH -> 180;
            default -> 0;
        }));
        float phi = -turbine.phi(partialTick);
        if (inWorld) {
            int stage = Math.min(turbine.stage(), STAGES - 1);
            renderAll(models[stage], stage, turbine.damage(), phi, poseStack, consumer, light, overlay);
        } else {
            // as an item: one small stage
            poseStack.scale(0.6F, 0.6F, 0.6F);
            poseStack.translate(-0.1, 0.8, 0);
            renderAll(models[0], 0, 0, phi, poseStack, consumer, light, overlay);
        }
        poseStack.popPose();
    }

    private void renderAll(ModelPart model, int stage, int damage, float phi, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0, AXIS_HEIGHT, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(phi));
        poseStack.translate(0, -AXIS_HEIGHT, 0);
        model.getChild("shaft1").render(poseStack, consumer, light, overlay);
        model.getChild("shaft1a").render(poseStack, consumer, light, overlay);
        // two rings of blades, the second a bit bigger
        poseStack.translate(0, 0, RING_SPACING);
        renderBlades(model, stage, damage, poseStack, consumer, light, overlay);
        poseStack.translate(0, 0, -RING_SPACING * 2);
        double scale = scaleFactor(stage);
        poseStack.translate(0, AXIS_HEIGHT, 0);
        poseStack.scale((float) scale, (float) scale, 1F);
        poseStack.translate(0, -AXIS_HEIGHT, 0);
        renderBlades(model, stage, damage, poseStack, consumer, light, overlay);
        poseStack.popPose();
    }

    private void renderBlades(ModelPart model, int stage, int damage, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        int step = angularSeparation(stage) * (damage + 1);
        ModelPart blade = model.getChild("blade");
        for (int angle = 0; angle < 360; angle += step) {
            poseStack.pushPose();
            poseStack.translate(0, AXIS_HEIGHT, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
            poseStack.translate(0, -AXIS_HEIGHT, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bladeTwist(stage)));
            blade.render(poseStack, consumer, light, overlay);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(TurbineCoreBlockEntity turbine) {
        return new AABB(turbine.getBlockPos()).inflate(6);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
