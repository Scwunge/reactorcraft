package net.scwunge.reactorcraft.content.machine;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;

import java.util.function.Supplier;

/** Waste Decayer: stacked decayers look like one tall chamber. */
public class WasteDecayerBlock extends StackableMachineBlock {
    public WasteDecayerBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type);
    }
}
