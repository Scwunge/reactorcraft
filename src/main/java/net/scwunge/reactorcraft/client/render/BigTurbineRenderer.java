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
import net.scwunge.reactorcraft.content.machine.BigTurbineBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;

/**
 * Draws a big turbine block (RenderBigTurbine and ModelBigTurbine): the small turbine's shaft and rings of blades, but longer blades, packed closer,
 * seven sizes, with a ring of housing plates round them that does not turn. The first stage also has its steam deflector. Wear thins the blades.
 */
public class BigTurbineRenderer implements BlockEntityRenderer<BigTurbineBlockEntity> {
    public static final int STAGES = 7;
    private static final double AXIS_HEIGHT = 0.9375;
    private static final double RING_SPACING = 0.25;
    private static final int[] BLADE_LENGTH = {20, 31, 38, 46, 53, 62, 72};
    private static final int[] SEPARATION = {4, 3, 3, 3, 3, 3, 4};
    private static final int[] HOUSING_SEGMENTS = {8, 10, 12, 12, 18, 18, 20};
    private static final int[] HOUSING_LENGTH = {9, 11, 11, 13, 10, 12, 12};
    private static final int[] HOUSING_DEPTH = {6, 6, 6, 7, 7, 8, 9};

    private final ModelPart[] models = new ModelPart[STAGES];
    private final ResourceLocation texture;

    public BigTurbineRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation[] layers, String texture) {
        for (int i = 0; i < STAGES; i++) {
            models[i] = context.bakeLayer(layers[i]);
        }
        this.texture = ReactorCraft.id("textures/entity/" + texture + ".png");
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
        double d = stage == 0 ? 1.3 : stage == 3 ? 1.075 : 1.1;
        return stage >= 5 ? d * (1 - (stage - 2) * 0.01) : d;
    }

    /** One stage's model: shaft, a blade sized for the stage, and the housing plates. */
    public static LayerDefinition createLayer(int stage) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("shaft1", CubeListBuilder.create().texOffs(0, 106).mirror().addBox(-2F, -2F, 0F, 4F, 4F, 16F),
                PartPose.offsetAndRotation(0F, 15F, -8F, 0F, 0F, 0.7853982F));
        root.addOrReplaceChild("shaft1a", CubeListBuilder.create().texOffs(0, 106).mirror().addBox(-2F, -2F, 0F, 4F, 4F, 16F),
                PartPose.offset(0F, 15F, -8F));
        root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0).mirror()
                        .addBox(-0.5F, -BLADE_LENGTH[stage], -bladeWidth(stage) / 2F, 1F, BLADE_LENGTH[stage], bladeWidth(stage)),
                PartPose.offset(0F, 15F, 0F));
        int w1 = HOUSING_LENGTH[stage];
        int w2 = stage == 0 ? 12 : HOUSING_LENGTH[stage] + 1;
        int l1 = BLADE_LENGTH[stage] + 2;
        int l2 = (int) Math.ceil(BLADE_LENGTH[stage] * scaleFactor(stage)) + 2;
        int depth = HOUSING_DEPTH[stage];
        root.addOrReplaceChild("housing", CubeListBuilder.create().texOffs(58, 11).mirror().addBox(-w1, l1, 0F, w1 * 2, 1F, depth), PartPose.offset(0F, 15F, 0F));
        root.addOrReplaceChild("housing2", CubeListBuilder.create().texOffs(58, 11).mirror().addBox(-w2, l2, -8F, w2 * 2, 1F, depth), PartPose.offset(0F, 15F, 0F));
        root.addOrReplaceChild("deflector", CubeListBuilder.create().texOffs(39, 10).mirror().addBox(-4F, -16F, 0F, 8F, 16F, 1F),
                PartPose.offsetAndRotation(0F, 34F, 6F, -0.3490659F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(BigTurbineBlockEntity turbine, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
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
            poseStack.scale(0.4F, 0.4F, 0.4F);
            poseStack.translate(-0.1, 1.6, 0);
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
        poseStack.translate(0, 0, RING_SPACING);
        renderBlades(model, stage, damage, poseStack, consumer, light, overlay);
        poseStack.translate(0, 0, -RING_SPACING * 2);
        double scale = scaleFactor(stage);
        poseStack.translate(0, AXIS_HEIGHT, 0);
        poseStack.scale((float) scale, (float) scale, 1F);
        poseStack.translate(0, -AXIS_HEIGHT, 0);
        renderBlades(model, stage, damage, poseStack, consumer, light, overlay);
        poseStack.popPose();
        renderHousing(model, stage, poseStack, consumer, light, overlay);
    }

    private void renderBlades(ModelPart model, int stage, int damage, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        int step = SEPARATION[stage] * (damage + 1);
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

    /** renderHousing: plates round the blades that stay still. */
    private void renderHousing(ModelPart model, int stage, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        int step = 360 / HOUSING_SEGMENTS[stage];
        for (int angle = 0; angle < 360; angle += step) {
            poseStack.pushPose();
            poseStack.translate(0, 1, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
            poseStack.translate(0, -1, 0);
            model.getChild("housing").render(poseStack, consumer, light, overlay);
            model.getChild("housing2").render(poseStack, consumer, light, overlay);
            if (stage == 0) {
                poseStack.translate(0, -0.3, -0.15);
                poseStack.scale(1.25F, 1.25F, 1.25F);
                model.getChild("deflector").render(poseStack, consumer, light, overlay);
            }
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(BigTurbineBlockEntity turbine) {
        return new AABB(turbine.getBlockPos()).inflate(8);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
