package net.dries007.tfc.world.feature;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.mojang.serialization.Codec;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;

public class NoisyMultipleFeature extends Feature<SimpleRandomFeatureConfiguration>
{
    public NoisyMultipleFeature(Codec<SimpleRandomFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SimpleRandomFeatureConfiguration> context)
    {
        final BlockPos pos = context.origin();
        final ChunkDataProvider provider = ChunkDataProvider.get(context.chunkGenerator());
        final ChunkData data = provider.get(context.level(), pos);
        final int rotation2 = (int) Math.ceil(data.getForestWeirdness() * context.config().features.size());

        List<Holder<PlacedFeature>> features = context.config().features.stream().collect(Collectors.toList());
        Collections.rotate(features, rotation2);

        int placed = 0;
        for (Holder<PlacedFeature> feature : context.config().features)
        {
            if (feature.value().placeWithBiomeCheck(context.level(), context.chunkGenerator(), context.random(), pos))
            {
                placed++;
                if (placed > 1)
                {
                    return true;
                }
            }
        }
        return placed > 0;
    }
}
