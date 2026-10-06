package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.scwunge.reactorcraft.client.model.ModelDiffuser;
import net.scwunge.reactorcraft.content.machine.SteamDiffuserBlockEntity;

/** The diffuser model, turned so its intake faces the steam line. */
public class SteamDiffuserRenderer extends ModelMachineRenderer<SteamDiffuserBlockEntity> {
    public SteamDiffuserRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer, "steam_diffuser", ModelDiffuser.PARTS);
    }

    @Override
    protected void orient(SteamDiffuserBlockEntity machine, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(machine.getLevel() == null ? 0 : machine.facing().toYRot()));
    }
}
