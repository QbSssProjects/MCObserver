package qbsss.mcDebugging.clientruntime;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VersionService {

    // Popularne wersje Minecraft z wsparciem dla modloaderów
    private static final Map<String, MinecraftVersion> VERSIONS = new LinkedHashMap<>();

    static {
        // Najnowsze wersje (1.20+)
        VERSIONS.put("1.21", new MinecraftVersion("1.21", "release", true, true));
        VERSIONS.put("1.20.6", new MinecraftVersion("1.20.6", "release", true, true));
        VERSIONS.put("1.20.4", new MinecraftVersion("1.20.4", "release", true, true));
        VERSIONS.put("1.20.2", new MinecraftVersion("1.20.2", "release", true, true));
        VERSIONS.put("1.20.1", new MinecraftVersion("1.20.1", "release", true, true));
        VERSIONS.put("1.20", new MinecraftVersion("1.20", "release", true, true));

        // 1.19.x
        VERSIONS.put("1.19.4", new MinecraftVersion("1.19.4", "release", true, true));
        VERSIONS.put("1.19.3", new MinecraftVersion("1.19.3", "release", true, true));
        VERSIONS.put("1.19.2", new MinecraftVersion("1.19.2", "release", true, true));
        VERSIONS.put("1.19.1", new MinecraftVersion("1.19.1", "release", true, true));
        VERSIONS.put("1.19", new MinecraftVersion("1.19", "release", true, true));

        // 1.18.x
        VERSIONS.put("1.18.2", new MinecraftVersion("1.18.2", "release", true, true));
        VERSIONS.put("1.18.1", new MinecraftVersion("1.18.1", "release", true, true));
        VERSIONS.put("1.18", new MinecraftVersion("1.18", "release", true, true));

        // 1.17.x
        VERSIONS.put("1.17.1", new MinecraftVersion("1.17.1", "release", true, true));
        VERSIONS.put("1.17", new MinecraftVersion("1.17", "release", true, true));

        // 1.16.x (bardzo popularna)
        VERSIONS.put("1.16.5", new MinecraftVersion("1.16.5", "release", true, true));
        VERSIONS.put("1.16.4", new MinecraftVersion("1.16.4", "release", true, true));
        VERSIONS.put("1.16.3", new MinecraftVersion("1.16.3", "release", true, true));
        VERSIONS.put("1.16.2", new MinecraftVersion("1.16.2", "release", true, true));
        VERSIONS.put("1.16.1", new MinecraftVersion("1.16.1", "release", true, true));

        // 1.15.x
        VERSIONS.put("1.15.2", new MinecraftVersion("1.15.2", "release", true, true));
        VERSIONS.put("1.15.1", new MinecraftVersion("1.15.1", "release", true, true));

        // 1.14.x
        VERSIONS.put("1.14.4", new MinecraftVersion("1.14.4", "release", true, true));

        // 1.12.x (legendarny Forge)
        VERSIONS.put("1.12.2", new MinecraftVersion("1.12.2", "release", true, true));
        VERSIONS.put("1.12.1", new MinecraftVersion("1.12.1", "release", false, true));

        // 1.11.x - 1.7.x (starsze wersje)
        VERSIONS.put("1.11.2", new MinecraftVersion("1.11.2", "release", false, true));
        VERSIONS.put("1.10.2", new MinecraftVersion("1.10.2", "release", false, true));
        VERSIONS.put("1.9.4", new MinecraftVersion("1.9.4", "release", false, true));
        VERSIONS.put("1.8.9", new MinecraftVersion("1.8.9", "release", false, true));
        VERSIONS.put("1.7.10", new MinecraftVersion("1.7.10", "release", false, true));
    }

    public List<MinecraftVersion> getAllVersions() {
        return new ArrayList<>(VERSIONS.values());
    }

    public MinecraftVersion getVersion(String version) {
        return VERSIONS.get(version);
    }

    public List<String> getSupportedModLoaders(String version) {
        MinecraftVersion mv = VERSIONS.get(version);
        if (mv == null) return Collections.emptyList();

        List<String> loaders = new ArrayList<>();
        loaders.add("VANILLA");
        if (mv.supportsFabric()) loaders.add("FABRIC");
        if (mv.supportsForge()) loaders.add("FORGE");
        return loaders;
    }

    // Najpopularniejsze wersje Fabric Loader
    public List<String> getFabricLoaderVersions() {
        return Arrays.asList(
            "0.15.11", "0.15.10", "0.15.9", "0.15.7", "0.15.6",
            "0.15.3", "0.15.2", "0.15.1", "0.15.0",
            "0.14.25", "0.14.24", "0.14.23", "0.14.22", "0.14.21",
            "0.14.19", "0.14.18", "0.14.17", "0.14.14", "0.14.11",
            "0.14.10", "0.14.9", "0.14.8", "0.14.6"
        );
    }

    // Rekomendowane wersje Forge dla popularnych wersji MC
    public Map<String, List<String>> getForgeVersions() {
        Map<String, List<String>> forgeVersions = new HashMap<>();

        forgeVersions.put("1.20.4", Arrays.asList("49.0.50", "49.0.48", "49.0.44"));
        forgeVersions.put("1.20.2", Arrays.asList("48.1.0", "48.0.49", "48.0.45"));
        forgeVersions.put("1.20.1", Arrays.asList("47.3.0", "47.2.21", "47.2.20", "47.2.17"));
        forgeVersions.put("1.20", Arrays.asList("46.0.14", "46.0.13"));
        forgeVersions.put("1.19.4", Arrays.asList("45.3.0", "45.2.11", "45.2.0"));
        forgeVersions.put("1.19.3", Arrays.asList("44.1.23", "44.1.16", "44.1.8"));
        forgeVersions.put("1.19.2", Arrays.asList("43.4.0", "43.3.13", "43.3.5", "43.2.23"));
        forgeVersions.put("1.19.1", Arrays.asList("42.0.9", "42.0.8"));
        forgeVersions.put("1.19", Arrays.asList("41.1.0", "41.0.110"));
        forgeVersions.put("1.18.2", Arrays.asList("40.2.21", "40.2.10", "40.2.9", "40.2.0"));
        forgeVersions.put("1.18.1", Arrays.asList("39.1.2", "39.1.0"));
        forgeVersions.put("1.18", Arrays.asList("38.0.17"));
        forgeVersions.put("1.17.1", Arrays.asList("37.1.1", "37.1.0"));
        forgeVersions.put("1.16.5", Arrays.asList("36.2.42", "36.2.39", "36.2.35", "36.2.34"));
        forgeVersions.put("1.16.4", Arrays.asList("35.1.37", "35.1.36"));
        forgeVersions.put("1.16.3", Arrays.asList("34.1.42"));
        forgeVersions.put("1.15.2", Arrays.asList("31.2.57"));
        forgeVersions.put("1.14.4", Arrays.asList("28.2.26"));
        forgeVersions.put("1.12.2", Arrays.asList("14.23.5.2860", "14.23.5.2859", "14.23.5.2855"));
        forgeVersions.put("1.11.2", Arrays.asList("13.20.1.2588"));
        forgeVersions.put("1.10.2", Arrays.asList("12.18.3.2511"));
        forgeVersions.put("1.8.9", Arrays.asList("11.15.1.2318"));
        forgeVersions.put("1.7.10", Arrays.asList("10.13.4.1614"));

        return forgeVersions;
    }

    public List<String> getForgeVersionsForMC(String mcVersion) {
        return getForgeVersions().getOrDefault(mcVersion, Collections.emptyList());
    }
}
