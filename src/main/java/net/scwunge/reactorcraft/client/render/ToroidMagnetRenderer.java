package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.client.model.ModelMagnet;
import net.scwunge.reactorcraft.content.machine.ToroidMagnetBlockEntity;

/** The toroid magnet model, turned to the way it steers plasma (RenderMagnet: 90 degrees minus the aim angle about the vertical). */
public class ToroidMagnetRenderer extends ModelMachineRenderer<ToroidMagnetBlockEntity> {
    public ToroidMagnetRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer, "toroid_magnet", ModelMagnet.PARTS);
    }

    @Override
    protected void orient(ToroidMagnetBlockEntity machine, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(90 - machine.aim().angle));
    }

    @Override
    public AABB getRenderBoundingBox(ToroidMagnetBlockEntity machine) {
        return new AABB(machine.getBlockPos()).inflate(3);
    }
}
