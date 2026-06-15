package ru.liko.tacz_mechanics.client;

import ru.liko.tacz_mechanics.Config;

/**
 * Effective distant_fire client settings for the muffle mixin.
 * On dedicated servers values arrive via {@link ru.liko.tacz_mechanics.network.DistantFireSyncPayload};
 * locally they come straight from {@link Config}.
 */
public final class ClientDistantFireSettings {
    private static volatile boolean enabled;
    private static volatile int nearSoundRange;
    private static volatile int maxDistance;
    private static volatile boolean soundPropagation;
    private static volatile double soundSpeedBlocksPerSecond;
    private static volatile int soundPropagationMaxDelayTicks;

    static {
        applyFromLocalConfig();
    }

    private ClientDistantFireSettings() {
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Radius at which TaCZ's 3rd-person muffling kicks in. */
    public static int nearSoundRange() {
        return nearSoundRange;
    }

    /** Distance at which the low-pass muffle reaches its maximum. */
    public static int muffleFarDistance() {
        return maxDistance;
    }

    public static boolean soundPropagation() {
        return soundPropagation;
    }

    public static double soundSpeedBlocksPerSecond() {
        return soundSpeedBlocksPerSecond;
    }

    /** 0 = no upper cap on propagation delay. */
    public static int soundPropagationMaxDelayTicks() {
        return soundPropagationMaxDelayTicks;
    }

    /** Game ticks before a distant shot is heard: distance / speed of sound, capped when configured. */
    public static int propagationDelayTicks(double distanceFromListenerBlocks) {
        if (!soundPropagation || !(distanceFromListenerBlocks > 0.0)) {
            return 0;
        }
        double speed = soundSpeedBlocksPerSecond;
        if (speed < 1e-6) {
            return 0;
        }
        int ticks = (int) Math.round((distanceFromListenerBlocks / speed) * 20.0);
        ticks = Math.max(0, ticks);
        int cap = soundPropagationMaxDelayTicks;
        if (cap > 0 && ticks > cap) {
            ticks = cap;
        }
        return ticks;
    }

    public static void applyFromLocalConfig() {
        enabled = Config.DistantFire.enabled;
        nearSoundRange = Config.DistantFire.nearSoundRange;
        maxDistance = Config.DistantFire.maxDistance;
        soundPropagation = Config.DistantFire.soundPropagation;
        soundSpeedBlocksPerSecond = Config.DistantFire.soundSpeedBlocksPerSecond;
        soundPropagationMaxDelayTicks = Config.DistantFire.soundPropagationMaxDelayTicks;
    }

    public static void applyFromServer(
        boolean distantEnabled,
        int nearRangeBlocks,
        int maxDistBlocks,
        boolean soundPropagationEnabled,
        double soundSpeedBps,
        int maxPropagationDelayTicks) {

        enabled = distantEnabled;
        nearSoundRange = nearRangeBlocks;
        maxDistance = maxDistBlocks;
        soundPropagation = soundPropagationEnabled;
        soundSpeedBlocksPerSecond = soundSpeedBps;
        soundPropagationMaxDelayTicks = maxPropagationDelayTicks;
    }
}
