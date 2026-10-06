package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.client.model.ModelProcessor;
import net.scwunge.reactorcraft.content.machine.ReactorMachineBlock;
import net.scwunge.reactorcraft.content.machine.UraniumProcessorBlockEntity;

/**
 * The Uranium Processor: the model turned to face its output side, with its three tanks (input, intermediate, output)
 * drawn as fluid volumes inside it. The fluid boxes use the original's sizes and offsets, per facing.
 */
public class ProcessorRenderer extends ModelMachineRenderer<UraniumProcessorBlockEntity> {
    public ProcessorRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer, "uranium_processor", ModelProcessor.PARTS);
    }

    /** The original's metadata for a facing: 0 west, 1 east, 2 north, 3 south. */
    private static int meta(Direction facing) {
        return switch (facing) {
            case WEST -> 0;
            case EAST -> 1;
            case NORTH -> 2;
            default -> 3;
        };
    }

    @Override
    protected void orient(UraniumProcessorBlockEntity machine, PoseStack poseStack) {
        poseStack.translate(0, 0.01, 0);
        float angle = switch (meta(facing(machine))) {
            case 0 -> 270;
            case 1 -> 90;
            case 2 -> 0;
            default -> 180;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
    }

    private static Direction facing(UraniumProcessorBlockEntity machine) {
        Direction look = machine.getBlockState().getValue(ReactorMachineBlock.LOOK);
        return look.getAxis().isHorizontal() ? look : Direction.SOUTH;
    }

    @Override
    protected void renderExtras(UraniumProcessorBlockEntity machine, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                int light, int overlay) {
        int meta = meta(facing(machine));
        FluidTank[] tanks = {machine.inputTank(), machine.intermediateTank(), machine.outputTank()};
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        for (int i = 0; i < 3; i++) {
            FluidTank tank = tanks[i];
            if (tank.isEmpty()) {
                continue;
            }
            double fill = Math.min(1, tank.getFluidAmount() / (double) tank.getCapacity());
            drawVolume(poseStack, consumer, tank.getFluid().getFluid(), i, meta, fill, light);
        }
    }

    private static double scaleX(int i, int meta) {
        if (i == 2) {
            return meta >= 2 ? 0.875 : 0.4375;
        }
        return meta >= 2 ? 0.5 : 0.5625;
    }

    private static double scaleZ(int i, int meta) {
        if (i == 2) {
            return meta >= 2 ? 0.4375 : 0.875;
        }
        return meta < 2 ? 0.5 : 0.5625;
    }

    private static double offsetX(int i, int meta) {
        if (i == 0 && meta == 0 || i == 1 && meta == 0) {
            return 0.775;
        }
        if (i == 0 && meta == 3 || i == 1 && meta == 2) {
            return 1;
        }
        if (i == 2 && meta >= 2) {
            return 0.0625;
        }
        if (i == 2 && meta == 1) {
            return 1.25;
        }
        return 0;
    }

    private static double offsetZ(int i, int meta) {
        if (i == 0 && meta == 2 || i == 1 && meta == 2) {
            return 0.775;
        }
        if (i == 0 && meta == 0 || i == 1 && meta == 1) {
            return 1;
        }
        if (i == 2 && meta == 3) {
            return 1.25;
        }
        if (i == 2 && meta < 2) {
            return 0.0625;
        }
        return 0;
    }

    /** One tank's fluid as a box: the original scaled and moved a unit cube, then shrank it to 99% / 98% and lifted it 0.01. */
    private void drawVolume(PoseStack poseStack, VertexConsumer consumer, Fluid fluid, int i, int meta, double fill, int light) {
        double sx = scaleX(i, meta);
        double sy = i == 2 ? 11 / 14D : 1;
        double sz = scaleZ(i, meta);
        double ox = offsetX(i, meta);
        double oz = offsetZ(i, meta);
        float x0 = (float) (sx * ox);
        float x1 = (float) (sx * (ox + 0.99));
        float z0 = (float) (sz * oz);
        float z1 = (float) (sz * (oz + 0.99));
        float y0 = (float) (sy * 0.01);
        float y1 = (float) (sy * (0.01 + 0.98 * fill));
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
        int tint = ext.getTintColor();
        float a = ((tint >> 24) & 0xFF) / 255F;
        float r = ((tint >> 16) & 0xFF) / 255F;
        float g = ((tint >> 8) & 0xFF) / 255F;
        float b = (tint & 0xFF) / 255F;
        a = Math.min(a == 0 ? 1 : a, 0.85F);
        // top
        quad(consumer, poseStack, sprite, r, g, b, a, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        // four sides
        quad(consumer, poseStack, sprite, r, g, b, a, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(consumer, poseStack, sprite, r, g, b, a, light, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        quad(consumer, poseStack, sprite, r, g, b, a, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quad(consumer, poseStack, sprite, r, g, b, a, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite sprite, float r, float g, float b, float a,
                             int light, float nx, float ny, float nz, float... c) {
        float[] u = {sprite.getU0(), sprite.getU0(), sprite.getU1(), sprite.getU1()};
        float[] v = {sprite.getV1(), sprite.getV0(), sprite.getV0(), sprite.getV1()};
        for (int k = 0; k < 4; k++) {
            consumer.addVertex(poseStack.last(), c[k * 3], c[k * 3 + 1], c[k * 3 + 2]).setColor(r, g, b, a).setUv(u[k], v[k])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(poseStack.last(), nx, ny, nz);
        }
    }
}
