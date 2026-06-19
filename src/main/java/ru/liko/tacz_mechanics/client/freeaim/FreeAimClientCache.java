package ru.liko.tacz_mechanics.client.freeaim;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of other players' free-aim offsets (for third-person rendering).
 */
public final class FreeAimClientCache {

    private record Entry(float pitch, float yaw, long timestamp) {}

    private static final Map<UUID, Entry> OFFSETS = new ConcurrentHashMap<>();
    private static final long TTL_MS = 1000L;

    private FreeAimClientCache() {
    }

    public static void update(UUID id, float pitch, float yaw) {
        OFFSETS.put(id, new Entry(pitch, yaw, System.currentTimeMillis()));
    }

    public static float getPitch(UUID id) {
        Entry e = OFFSETS.get(id);
        if (e == null || System.currentTimeMillis() - e.timestamp() > TTL_MS) {
            return 0f;
        }
        return e.pitch();
    }

    public static float getYaw(UUID id) {
        Entry e = OFFSETS.get(id);
        if (e == null || System.currentTimeMillis() - e.timestamp() > TTL_MS) {
            return 0f;
        }
        return e.yaw();
    }

    public static void remove(UUID id) {
        OFFSETS.remove(id);
    }

    public static void clear() {
        OFFSETS.clear();
    }
}
