package ru.liko.tacz_mechanics.client.sound;

import net.minecraft.client.resources.sounds.SoundInstance;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for tracking sounds that need filters applied. Entries are kept until the
 * low-pass filter is successfully attached to the OpenAL source, or the per-entry
 * retry budget is exhausted (e.g. the {@link SoundSourceTracker} never resolved a
 * channel for the instance), so the list cannot grow without bound.
 */
public class SoundFilterRegistry {

    /** Hard upper bound on per-tick retries before we give up on a stuck sound. */
    private static final int MAX_RETRIES = 60;

    private static final List<MuffledSound> trackedSounds = new CopyOnWriteArrayList<>();

    public static void register(SoundInstance sound, float muffleAmount) {
        trackedSounds.add(new MuffledSound(sound, muffleAmount));
    }

    public static List<MuffledSound> getAll() {
        return Collections.unmodifiableList(trackedSounds);
    }

    public static void tick() {
        for (MuffledSound muffled : trackedSounds) {
            if (muffled.filterApplied) {
                continue;
            }
            if (SoundFilterUtil.applyLowPassFilter(muffled.sound, muffled.muffleAmount)) {
                muffled.filterApplied = true;
            } else {
                muffled.attempts++;
            }
        }
        trackedSounds.removeIf(m -> m.filterApplied || m.attempts >= MAX_RETRIES);
    }

    public static void cleanup() {
        trackedSounds.clear();
    }

    public static class MuffledSound {
        public final SoundInstance sound;
        public final float muffleAmount;
        public boolean filterApplied = false;
        public int attempts = 0;

        public MuffledSound(SoundInstance sound, float muffleAmount) {
            this.sound = sound;
            this.muffleAmount = muffleAmount;
        }
    }
}
