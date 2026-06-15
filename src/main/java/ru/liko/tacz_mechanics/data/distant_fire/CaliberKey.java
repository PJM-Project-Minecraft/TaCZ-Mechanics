package ru.liko.tacz_mechanics.data.distant_fire;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Normalises caliber identifiers to a small set of equivalent lookup keys.
 *
 * <p>TaCZ ammo IDs come in shapes like {@code tacz:9mm}, {@code tacz:30_06}, {@code tacz:556x45}.
 * Datapacks use either the full ID or just the path, with arbitrary {@code _} vs {@code -}
 * spelling. {@link #candidates(String)} returns the deduplicated list of keys to try when
 * registering or looking up a caliber so the registry doesn't need ad-hoc string juggling.
 */
public final class CaliberKey {
    private CaliberKey() {
    }

    /**
     * Build the ordered set of equivalent keys for {@code raw}, e.g.
     * {@code "tacz:30_06"} → {@code ["tacz:30_06", "30_06", "tacz:30-06", "30-06"]}.
     * The first element is always {@code raw} (whitespace-trimmed). Returns an empty set for
     * {@code null} or blank input.
     */
    public static Set<String> candidates(String raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null) {
            return out;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return out;
        }
        addVariants(out, trimmed);

        int colon = trimmed.indexOf(':');
        if (colon >= 0 && colon < trimmed.length() - 1) {
            addVariants(out, trimmed.substring(colon + 1));
        }

        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0 && slash < trimmed.length() - 1) {
            addVariants(out, trimmed.substring(slash + 1));
        }

        return out;
    }

    private static void addVariants(Set<String> out, String value) {
        if (value.isEmpty()) {
            return;
        }
        out.add(value);
        out.add(value.replace('_', '-'));
        out.add(value.replace('-', '_'));
    }
}
