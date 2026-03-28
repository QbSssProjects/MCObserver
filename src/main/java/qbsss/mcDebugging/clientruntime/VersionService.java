package qbsss.mcDebugging.clientruntime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class VersionService {
    private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AtomicReference<List<MinecraftVersion>> cachedVersions = new AtomicReference<>(List.of());

    public List<MinecraftVersion> getAllVersions() {
        List<MinecraftVersion> versions = cachedVersions.get();
        if (!versions.isEmpty()) {
            return versions;
        }

        List<MinecraftVersion> fetched = fetchAllVersions();
        cachedVersions.compareAndSet(List.of(), fetched);
        return cachedVersions.get();
    }

    public MinecraftVersion getVersion(String version) {
        return getAllVersions().stream()
            .filter(v -> v.getVersion().equals(version))
            .findFirst()
            .orElse(null);
    }

    public List<String> getSupportedModLoaders(String version) {
        MinecraftVersion mv = getVersion(version);
        if (mv == null) {
            return Collections.emptyList();
        }

        List<String> loaders = new ArrayList<>();
        loaders.add("VANILLA");
        if (mv.supportsFabric()) {
            loaders.add("FABRIC");
        }
        if (mv.supportsForge()) {
            loaders.add("FORGE");
        }
        return loaders;
    }

    public List<String> getFabricLoaderVersions() {
        return Arrays.asList(
            "0.15.11", "0.15.10", "0.15.9", "0.15.7", "0.15.6",
            "0.15.3", "0.15.2", "0.15.1", "0.15.0",
            "0.14.25", "0.14.24", "0.14.23", "0.14.22", "0.14.21",
            "0.14.19", "0.14.18", "0.14.17", "0.14.14", "0.14.11",
            "0.14.10", "0.14.9", "0.14.8", "0.14.6"
        );
    }

    public Map<String, List<String>> getForgeVersions() {
        Map<String, List<String>> forgeVersions = new HashMap<>();
        forgeVersions.put("1.20.4", Arrays.asList("49.0.50", "49.0.48", "49.0.44"));
        forgeVersions.put("1.20.2", Arrays.asList("48.1.0", "48.0.49", "48.0.45"));
        forgeVersions.put("1.20.1", Arrays.asList("47.3.0", "47.2.21", "47.2.20", "47.2.17"));
        forgeVersions.put("1.20", Arrays.asList("46.0.14", "46.0.13"));
        forgeVersions.put("1.19.4", Arrays.asList("45.3.0", "45.2.11", "45.2.0"));
        forgeVersions.put("1.19.3", Arrays.asList("44.1.23", "44.1.16", "44.1.8"));
        forgeVersions.put("1.19.2", Arrays.asList("43.4.0", "43.3.13", "43.3.5", "43.2.23"));
        forgeVersions.put("1.18.2", Arrays.asList("40.2.21", "40.2.10", "40.2.9", "40.2.0"));
        forgeVersions.put("1.16.5", Arrays.asList("36.2.42", "36.2.39", "36.2.35", "36.2.34"));
        forgeVersions.put("1.12.2", Arrays.asList("14.23.5.2860", "14.23.5.2859", "14.23.5.2855"));
        forgeVersions.put("1.7.10", Arrays.asList("10.13.4.1614"));
        return forgeVersions;
    }

    public List<String> getForgeVersionsForMC(String mcVersion) {
        return getForgeVersions().getOrDefault(mcVersion, Collections.emptyList());
    }

    private List<MinecraftVersion> fetchAllVersions() {
        try (InputStream inputStream = new URL(VERSION_MANIFEST_URL).openStream()) {
            JsonNode manifest = OBJECT_MAPPER.readTree(inputStream);
            JsonNode versions = manifest.get("versions");
            if (versions == null || !versions.isArray()) {
                return fallbackVersions();
            }

            List<MinecraftVersion> result = new ArrayList<>();
            for (JsonNode versionNode : versions) {
                String id = versionNode.get("id").asText();
                String type = versionNode.get("type").asText();
                result.add(new MinecraftVersion(id, type, supportsFabric(id, type), supportsForge(id, type)));
            }

            return result;
        } catch (Exception e) {
            return fallbackVersions();
        }
    }

    private boolean supportsFabric(String id, String type) {
        if (!("release".equals(type) || "snapshot".equals(type))) {
            return false;
        }
        return parseMinorVersion(id) >= 14;
    }

    private boolean supportsForge(String id, String type) {
        if (!("release".equals(type) || "snapshot".equals(type))) {
            return false;
        }
        int minor = parseMinorVersion(id);
        return minor >= 7;
    }

    private int parseMinorVersion(String id) {
        try {
            if (!id.startsWith("1.")) {
                return -1;
            }
            String[] split = id.split("\\.");
            if (split.length < 2) {
                return -1;
            }
            return Integer.parseInt(split[1].replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private List<MinecraftVersion> fallbackVersions() {
        return List.of(
            new MinecraftVersion("1.21", "release", true, true),
            new MinecraftVersion("1.20.4", "release", true, true),
            new MinecraftVersion("1.16.5", "release", true, true),
            new MinecraftVersion("1.12.2", "release", false, true),
            new MinecraftVersion("1.7.10", "release", false, true),
            new MinecraftVersion("inf-20100618", "old_alpha", false, false)
        );
    }
}
