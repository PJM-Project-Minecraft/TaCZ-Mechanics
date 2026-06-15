package ru.liko.tacz_mechanics.data.distant_fire;

/**
 * Distance partition for distant-fire layers. All values are in blocks.
 *
 * <p>{@code taczNearRange} is the radius up to which TaCZ plays its native shot; below it, no
 * distant layer is played. {@code upperBounds} is a strictly increasing list of layer upper
 * bounds: {@code upperBounds[i]} is the maximum distance covered by {@code layers[i]}. The
 * last layer covers everything beyond {@code upperBounds[upperBounds.length - 1]} and so the
 * array length must equal {@code layerCount - 1}. {@code transitionBlocks} is the half-width of
 * the crossfade window applied around each boundary (including {@code taczNearRange}).
 */
public record DistantFireBands(int taczNearRange, int[] upperBounds, int transitionBlocks) {

    public DistantFireBands {
        upperBounds = upperBounds.clone();
    }

    @Override
    public int[] upperBounds() {
        return upperBounds.clone();
    }

    int rawUpperBound(int index) {
        return upperBounds[index];
    }

    int boundaryCount() {
        return upperBounds.length;
    }

    /**
     * Adapt this band partition to a sound with {@code layerCount} layers.
     *
     * <p>If too few bounds are configured, the last bound is repeated (or {@code taczNearRange}
     * is used as a sane minimum). If too many are configured, the tail is dropped. The returned
     * instance always has {@code upperBounds().length == max(0, layerCount - 1)} and a strictly
     * non-decreasing sequence.
     */
    public DistantFireBands clampToLayers(int layerCount) {
        int needed = Math.max(0, layerCount - 1);
        if (upperBounds.length == needed) {
            return this;
        }
        int[] clamped = new int[needed];
        int prev = taczNearRange;
        for (int i = 0; i < needed; i++) {
            int v = i < upperBounds.length ? upperBounds[i] : prev;
            if (v < prev) {
                v = prev;
            }
            clamped[i] = v;
            prev = v;
        }
        return new DistantFireBands(taczNearRange, clamped, transitionBlocks);
    }

    /**
     * Maximum bound across all layers (or {@code taczNearRange} if there are no boundaries).
     */
    public int maxBound() {
        return upperBounds.length == 0 ? taczNearRange : upperBounds[upperBounds.length - 1];
    }
}
