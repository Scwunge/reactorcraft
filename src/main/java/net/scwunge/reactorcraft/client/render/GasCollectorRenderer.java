package net.scwunge.reactorcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.scwunge.reactorcraft.client.model.ModelGasCollector;
import net.scwunge.reactorcraft.content.machine.GasCollectorBlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;

/**
 * The Gas Collector: the model is built with its intake frame facing down, so it is turned to face the side it reads
 * from. The frame is only drawn when there is a furnace or refrigerator there, and a fading outline shows the block it reads.
 */
public class GasCollectorRenderer extends ModelMachineRenderer<GasCollectorBlockEntity> {
    private static final Set<String> FRAME = Set.of("shape2", "shape2a", "shape3", "shape3a");

    public GasCollectorRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer, "gas_collector", ModelGasCollector.PARTS);
    }

    @Override
    protected void orientInBlock(GasCollectorBlockEntity machine, PoseStack poseStack) {
        Direction read = machine.readDirection();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, -1, 0), new Vector3f(read.getStepX(), read.getStepY(), read.getStepZ())));
        poseStack.translate(-0.5, -0.5, -0.5);
    }

    @Override
    protected boolean shows(GasCollectorBlockEntity machine, String part) {
        return !FRAME.contains(part) || machine.getLevel() != null && machine.hasFurnace();
    }

    @Override
    protected void renderExtras(GasCollectorBlockEntity machine, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
                                int overlay) {
        if (machine.getLevel() == null || machine.ticks <= 0) {
            return;
        }
        Direction read = machine.readDirection();
        AABB box = new AABB(read.getStepX(), read.getStepY(), read.getStepZ(), read.getStepX() + 1, read.getStepY() + 1, read.getStepZ() + 1)
                .inflate(0.03125);
        LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), box, 0F, 0.5F, 1F, machine.ticks / 512F);
    }
}
