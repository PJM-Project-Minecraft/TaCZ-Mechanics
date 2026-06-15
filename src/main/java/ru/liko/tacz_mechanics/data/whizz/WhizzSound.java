package ru.liko.tacz_mechanics.data.whizz;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Configuration for bullet whizz sounds.
 * Plays different sounds based on how close the bullet passes to a player.
 */
public record WhizzSound(
    double minSpeed,
    List<DistanceSound> sounds
) {
    public record DistanceSound(
        double threshold,
        ResourceLocation sound,
        float volume,
        float pitch
    ) {
        public static DistanceSound fromJson(JsonObject json) {
            double threshold = GsonHelper.getAsDouble(json, "threshold", 5.0);
            String soundStr = GsonHelper.getAsString(json, "sound");
            float volume = GsonHelper.getAsFloat(json, "volume", 1.0f);
            float pitch = GsonHelper.getAsFloat(json, "pitch", 1.0f);
            return new DistanceSound(threshold, ResourceLocation.parse(soundStr), volume, pitch);
        }
    }
    
    public static WhizzSound fromJson(JsonObject json) {
        double minSpeed = GsonHelper.getAsDouble(json, "min_speed", 5.0);
        
        List<DistanceSound> sounds = new ArrayList<>();
        if (json.has("sounds")) {
            for (var element : GsonHelper.getAsJsonArray(json, "sounds")) {
                sounds.add(DistanceSound.fromJson(element.getAsJsonObject()));
            }
        }
        // Sort by threshold ascending
        sounds.sort(Comparator.comparingDouble(DistanceSound::threshold));
        
        return new WhizzSound(minSpeed, sounds);
    }
    
    /**
     * Closest point on trajectory to the player (blocks). Uses the smallest tier whose threshold is still
     * not below that distance; if the bullet passes farther than all thresholds, the farthest tier is used.
     */
    public DistanceSound getSoundForDistance(double distance) {
        if (sounds.isEmpty()) {
            return null;
        }
        DistanceSound farthest = sounds.getLast();
        for (DistanceSound sound : sounds) {
            if (distance <= sound.threshold()) {
                return sound;
            }
        }
        return farthest;
    }
    
    public static final WhizzSound DEFAULT = new WhizzSound(1.0, List.of(
        new DistanceSound(2.0, ResourceLocation.parse("tacz_mechanics:generic.whizz.near"), 1.0f, 1.0f),
        new DistanceSound(5.0, ResourceLocation.parse("tacz_mechanics:generic.whizz.mid"), 1.0f, 1.0f),
        new DistanceSound(10.0, ResourceLocation.parse("tacz_mechanics:generic.whizz.far"), 1.0f, 1.0f)
    ));
}
