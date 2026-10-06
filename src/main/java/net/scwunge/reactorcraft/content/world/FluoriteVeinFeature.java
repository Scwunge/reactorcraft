package net.scwunge.reactorcraft.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.scwunge.reactorcraft.content.material.FluoriteColor;
import net.scwunge.reactorcraft.registry.ReactorBlocks;

import java.util.List;
import java.util.Optional;

/**
 * Fluorite veins (size 8). As in the original, every vein in a chunk has the same colour, chosen at random per chunk.
 */
public class FluoriteVeinFeature extends Feature<NoneFeatureConfiguration> {
    private static final int VEIN_SIZE = 8;

    public FluoriteVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        ChunkPos chunk = new ChunkPos(origin);
        RandomSource colorRandom = RandomSource.create(context.level().getSeed() ^ chunk.toLong() * 0x9E3779B97F4A7C15L);
        FluoriteColor color = FluoriteColor.values()[colorRandom.nextInt(FluoriteColor.values().length)];
        OreConfiguration ore = new OreConfiguration(List.of(OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
                ReactorBlocks.fluoriteOre(color).defaultBlockState())), VEIN_SIZE);
        return Feature.ORE.place(new FeaturePlaceContext<>(Optional.empty(), context.level(), context.chunkGenerator(), context.random(), origin, ore));
    }
}
