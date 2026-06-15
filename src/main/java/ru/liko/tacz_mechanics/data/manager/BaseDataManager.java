package ru.liko.tacz_mechanics.data.manager;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.data.core.Target;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory data manager backed by JSON files bundled inside the mod jar. Configs
 * are loaded exactly once during {@link #bootstrap()} (called from the mod
 * constructor) and are not affected by datapacks or {@code /reload}.
 */
public abstract class BaseDataManager<E> {
    protected final Logger logger = LogUtils.getLogger();
    protected Map<Class<?>, Map<ResourceLocation, E>> dataMap = Map.of();
    private boolean hasError = false;
    private boolean loaded = false;
    private final String directory;
    private final Comparator<E> comparator;
    private final Codec<E> codec;
    private final String[] knownFiles;

    protected BaseDataManager(String directory, Comparator<E> comparator, Codec<E> codec, String... knownFiles) {
        this.directory = directory;
        this.comparator = comparator;
        this.codec = codec;
        this.knownFiles = knownFiles;
    }

    public boolean hasError() {
        return hasError;
    }

    @SuppressWarnings("unchecked")
    protected <T extends E> Map<ResourceLocation, T> byType(Class<T> type) {
        return (Map<ResourceLocation, T>) dataMap.getOrDefault(type, Map.of());
    }

    /**
     * Reads every {@code data/<modid>/<directory>/**.json} file from the mod jar
     * and parses it through the configured {@link Codec}. Safe to call multiple
     * times (subsequent calls are ignored).
     */
    public final void bootstrap() {
        if (loaded) {
            return;
        }
        loaded = true;

        Map<ResourceLocation, JsonElement> elements = JsonResourceLoader.loadAll(TaczMechanics.MODID, directory, knownFiles);
        applyEntries(elements);
    }

    private void applyEntries(Map<ResourceLocation, JsonElement> elements) {
        hasError = false;
        Map<Class<?>, ImmutableMap.Builder<ResourceLocation, E>> data = new HashMap<>();

        for (var entry : elements.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                E element = codec.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(error -> new RuntimeException("Failed to parse " + id + ": " + error));

                data.computeIfAbsent(element.getClass(), k -> ImmutableMap.builder())
                    .put(id, element);

                logger.debug("Loaded {} from {}", element.getClass().getSimpleName(), id);
            } catch (RuntimeException e) {
                logger.error("Parsing error loading {}", id, e);
                hasError = true;
            }
        }

        Map<Class<?>, Map<ResourceLocation, E>> result = new HashMap<>();
        for (var entry : data.entrySet()) {
            result.put(entry.getKey(), entry.getValue()
                .orderEntriesByValue(comparator)
                .build());
        }
        dataMap = Map.copyOf(result);

        logger.info("[TaczMechanics] {}: loaded {} entries", directory, elements.size());
    }

    protected static boolean matchesTarget(List<Target> targets, ResourceLocation weaponId,
                                           ResourceLocation ammoId, float damage) {
        if (targets.isEmpty()) return true;
        return targets.stream().anyMatch(t -> t.test(weaponId, ammoId, damage));
    }
}
