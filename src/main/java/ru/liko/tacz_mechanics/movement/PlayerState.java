package ru.liko.tacz_mechanics.movement;

import ru.liko.tacz_mechanics.Config;

import java.util.concurrent.TimeUnit;

/**
 * Represents the movement state of a player.
 * Tracks the leaning (probe) state only.
 */
public class PlayerState {
    private byte probe = 0;
    private float probeOffset = 0;
    private float probeOffsetOld = 0;
    private long lastSyncTime = System.currentTimeMillis();

    public byte getProbe() { return probe; }
    public float getProbeOffset() { return probeOffset; }
    public float getProbeOffsetOld() { return probeOffsetOld; }

    private long lastProbe;

    public void updateOffset() {
        probeOffsetOld = probeOffset;
        double amplifier = (System.currentTimeMillis() - lastSyncTime) * (60 / 1000d);
        lastSyncTime = System.currentTimeMillis();

        // Slower speed for more realistic leaning (0.05 instead of 0.1)
        float speed = 0.05f;

        if (probe == -1) {
            if (probeOffset > -1) {
                probeOffset -= speed * amplifier;
            }
            if (probeOffset < -1) {
                probeOffset = -1;
            }
        }
        if (probe == 1) {
            if (probeOffset < 1) {
                probeOffset += speed * amplifier;
            }
            if (probeOffset > 1) {
                probeOffset = 1;
            }
        }

        if (probe == 0) {
            if (Math.abs(probeOffset) <= speed * amplifier) {
                probeOffset = 0;
            }
            if (probeOffset < 0) {
                probeOffset += speed * amplifier;
            }
            if (probeOffset > 0) {
                probeOffset -= speed * amplifier;
            }
        }
    }

    /**
     * Ограничивает анимированный наклон по максимальной величине в каждую сторону (из коллизии).
     */
    public void clampProbeOffset(float maxLeftMag, float maxRightMag) {
        if (probeOffset < 0) {
            if (probeOffset < -maxLeftMag) {
                probeOffset = -maxLeftMag;
            }
        } else if (probeOffset > 0) {
            if (probeOffset > maxRightMag) {
                probeOffset = maxRightMag;
            }
        }
    }

    public boolean canProbe() {
        return (System.currentTimeMillis() - this.lastProbe > TimeUnit.MILLISECONDS.toMillis((long)(Config.Movement.leanCooldown * 1000)));
    }

    public void resetProbe() {
        probe = 0;
    }

    public void leftProbe() {
        probe = -1;
        this.lastProbe = System.currentTimeMillis();
    }

    public void rightProbe() {
        probe = 1;
        this.lastProbe = System.currentTimeMillis();
    }

    public void readCode(int code) {
        probe = (byte) (code - 1);
    }

    public void reset() {
        readCode(1);
    }

    public int writeCode() {
        return probe + 1;
    }
}
