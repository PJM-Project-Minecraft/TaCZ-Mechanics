package ru.liko.tacz_mechanics.data.whizz;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.data.manager.JsonResourceLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages whizz sound configurations bundled inside the mod jar.
 * Configs live in {@code data/<modid>/whizz/*.json} and are loaded once at startup.
 */
public class WhizzSoundManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "whizz";

    public static final WhizzSoundManager INSTANCE = new WhizzSoundManager();

    private final Map<ResourceLocation, WhizzSound> configs = new HashMap<>();
    private WhizzSound defaultConfig = WhizzSound.DEFAULT;
    private boolean loaded = false;

    private WhizzSoundManager() {
    }

    public synchronized void bootstrap() {
        if (loaded) {
            return;
        }
        loaded = true;

        configs.clear();
        Map<ResourceLocation, JsonElement> resources = JsonResourceLoader.loadAll(
            TaczMechanics.MODID, DIRECTORY, "default");
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                WhizzSound config = WhizzSound.fromJson(json);

                String configId = id.getPath();
                if (configId.contains("/")) {
                    configId = configId.substring(configId.lastIndexOf('/') + 1);
                }

                ResourceLocation configKey = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), configId);
                configs.put(configKey, config);

                if (configId.equals("default")) {
                    defaultConfig = config;
                }

                LOGGER.debug("[WhizzSound] Loaded config: {} with {} sounds", configKey, config.sounds().size());
            } catch (Exception e) {
                LOGGER.error("[WhizzSound] Failed to load config: {}", id, e);
            }
        }

        LOGGER.info("[TaczMechanics] whizz: loaded {} entries", configs.size());
    }

    public WhizzSound getConfig(ResourceLocation id) {
        return configs.getOrDefault(id, defaultConfig);
    }

    public WhizzSound getDefaultConfig() {
        return defaultConfig;
    }
}
