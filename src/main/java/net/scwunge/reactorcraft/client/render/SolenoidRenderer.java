package net.scwunge.reactorcraft.client.render;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.client.model.ModelSolenoid;
import net.scwunge.reactorcraft.content.machine.SolenoidBlockEntity;

/** The solenoid's coil: the whole model, turning with the shaft, drawn only once the coil is built round it. */
public class SolenoidRenderer extends ModelMachineRenderer<SolenoidBlockEntity> {
    public SolenoidRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer, "solenoid_magnet", ModelSolenoid.PARTS, ModelSolenoid.PARTS);
    }

    @Override
    protected boolean shows(SolenoidBlockEntity machine, String part) {
        return machine.getLevel() == null || machine.isFormed();
    }

    @Override
    public AABB getRenderBoundingBox(SolenoidBlockEntity machine) {
        return machine.renderBounds();
    }
}
