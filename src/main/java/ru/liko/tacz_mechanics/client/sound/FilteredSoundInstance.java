package ru.liko.tacz_mechanics.client.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A sound instance that should have a low-pass filter applied.
 *
 * <p>Distant-fire sounds are dispatched with {@link Attenuation#NONE} so the OpenAL
 * source is forced relative to the listener (volume is fully driven by the server-
 * resolved value). This bypasses third-party acoustics mods (Sound Physics Remastered,
 * AmbientSounds) that would otherwise occlude or distance-mute the sound and make it
 * inaudible at the 60+ block ranges this system is designed for. Directionality is
 * sacrificed in exchange for guaranteed audibility.
 *
 * <p>Distant-fire playback may use {@code delayTicks} so the start matches sound travel time
 * (see {@link ru.liko.tacz_mechanics.client.ClientDistantFireSettings#propagationDelayTicks}).
 */
public class FilteredSoundInstance extends AbstractSoundInstance {
    private final float muffleAmount;
    private boolean filterApplied = false;

    public FilteredSoundInstance(SoundEvent sound, SoundSource source,
                                  float volume, float pitch,
                                  double x, double y, double z,
                                  float muffleAmount) {
        this(sound, source, volume, pitch, RandomSource.create(), x, y, z, muffleAmount, 0);
    }

    public FilteredSoundInstance(SoundEvent sound, SoundSource source,
                                  float volume, float pitch,
                                  double x, double y, double z,
                                  float muffleAmount,
                                  int delayTicks) {
        this(sound, source, volume, pitch, RandomSource.create(), x, y, z, muffleAmount, delayTicks);
    }

    public FilteredSoundInstance(SoundEvent sound, SoundSource source,
                                  float volume, float pitch,
                                  RandomSource random,
                                  double x, double y, double z,
                                  float muffleAmount,
                                  int delayTicks) {
        super(sound, source, random);
        this.volume = volume;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.muffleAmount = muffleAmount;
        this.looping = false;
        this.delay = Math.max(0, delayTicks);
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    public float getMuffleAmount() {
        return muffleAmount;
    }

    public boolean isFilterApplied() {
        return filterApplied;
    }

    public void setFilterApplied(boolean applied) {
        this.filterApplied = applied;
    }
}
