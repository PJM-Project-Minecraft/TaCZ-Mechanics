package ru.liko.tacz_mechanics.data.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads bundled JSON files directly from the mod's own jar / resource folder, without
 * relying on Minecraft's datapack reload system. Configurations are loaded once at
 * mod startup and cannot be overridden via datapacks.
 *
 * <p>Two strategies are used so we are robust both in dev (exploded resources) and in
 * production (NeoForge {@code SecureJar}):
 * <ol>
 *   <li>Walking the directory via {@link net.neoforged.neoforgespi.locating.IModFile#findResource}
 *       picks up every {@code *.json} file (including ones we did not anticipate).</li>
 *   <li>If walking yields nothing (e.g. the classloader does not expose directory
 *       entries), we fall back to {@link Class#getResourceAsStream(String)} for each
 *       explicitly known filename passed by the caller.</li>
 * </ol>
 */
public final class JsonResourceLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private JsonResourceLoader() {
    }

    /**
     * Loads every {@code *.json} found by walking
     * {@code data/<namespace>/<directory>/} inside the mod jar. Also tries to load
     * each name in {@code knownFiles} as a fallback (without {@code .json} extension)
     * if walking found nothing or skipped them.
     */
    public static Map<ResourceLocation, JsonElement> loadAll(String namespace, String directory,
                                                             String... knownFiles) {
        Map<ResourceLocation, JsonElement> result = new LinkedHashMap<>();
        String relativePath = "data/" + namespace + "/" + directory;

        walk(namespace, relativePath, result);

        for (String knownFile : knownFiles) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, knownFile);
            if (result.containsKey(id)) {
                continue;
            }
            String resourcePath = "/" + relativePath + "/" + knownFile + ".json";
            try (InputStream in = JsonResourceLoader.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    LOGGER.debug("[TaczMechanics] Bundled file not found: {}", resourcePath);
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    result.put(id, JsonParser.parseReader(reader));
                }
            } catch (IOException e) {
                LOGGER.error("[TaczMechanics] Failed to read {}", resourcePath, e);
            }
        }

        if (result.isEmpty()) {
            LOGGER.warn("[TaczMechanics] No bundled JSON entries found under {}", relativePath);
        } else {
            LOGGER.info("[TaczMechanics] Loaded {} bundled entries from {}", result.size(), relativePath);
        }

        return result;
    }

    private static void walk(String namespace, String relativePath, Map<ResourceLocation, JsonElement> sink) {
        Optional<Path> root = locateModResource(namespace, relativePath);
        if (root.isEmpty()) {
            return;
        }
        Path basePath = root.get();
        if (!Files.isDirectory(basePath)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(basePath)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .forEach(p -> readEntry(namespace, basePath, p, sink));
        } catch (IOException e) {
            LOGGER.warn("[TaczMechanics] Walk failed for {}: {}", basePath, e.toString());
        }
    }

    private static Optional<Path> locateModResource(String modId, String relativePath) {
        return ModList.get().getModContainerById(modId)
            .map(container -> container.getModInfo())
            .map(IModInfo::getOwningFile)
            .map(IModFileInfo::getFile)
            .map(file -> file.findResource(relativePath));
    }

    private static void readEntry(String namespace, Path root, Path file,
                                  Map<ResourceLocation, JsonElement> sink) {
        Path relative = root.relativize(file);
        String relativeStr = relative.toString().replace('\\', '/');
        if (!relativeStr.endsWith(".json")) {
            return;
        }
        String pathWithoutExt = relativeStr.substring(0, relativeStr.length() - ".json".length());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            JsonElement element = JsonParser.parseReader(reader);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, pathWithoutExt);
            sink.put(id, element);
        } catch (IOException e) {
            LOGGER.error("[TaczMechanics] Failed to read {}", file, e);
        }
    }
}
