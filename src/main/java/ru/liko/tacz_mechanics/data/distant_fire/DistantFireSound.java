package ru.liko.tacz_mechanics.data.distant_fire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Datapack-loaded distant fire configuration for a caliber.
 *
 * Pure data: only sounds and volumes per layer. Distance thresholds and crossfade transition
 * width live in {@link DistantFireBands} and are sent from server config; resolution logic
 * is in {@link DistantFireResolver}.
 */
public record DistantFireSound(String caliberId, List<DistantFireLayer> layers) {
    private static final Codec<DistantFireSound> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("caliber_id").forGetter(DistantFireSound::caliberId),
        DistantFireLayer.CODEC.listOf().fieldOf("layers").forGetter(DistantFireSound::layers)
    ).apply(instance, DistantFireSound::new));

    public static final Codec<DistantFireSound> CODEC = RAW_CODEC.validate(s -> s.layers().isEmpty()
        ? DataResult.error(() -> "DistantFireSound '" + s.caliberId() + "' must have at least one layer")
        : DataResult.success(s));

    public DistantFireSound {
        layers = List.copyOf(layers);
    }

    public DistantFireLayer layer(int index) {
        return layers.get(index);
    }

    public int layerCount() {
        return layers.size();
    }
}
