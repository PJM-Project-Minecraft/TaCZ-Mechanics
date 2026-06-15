package ru.liko.tacz_mechanics.data.distant_fire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * Single distant-fire layer: a sound and its base volume multiplier.
 */
public record DistantFireLayer(ResourceLocation sound, float volume) {
    public static final Codec<DistantFireLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("sound").forGetter(DistantFireLayer::sound),
        Codec.FLOAT.optionalFieldOf("volume", 1.0f).forGetter(DistantFireLayer::volume)
    ).apply(instance, DistantFireLayer::new));
}
