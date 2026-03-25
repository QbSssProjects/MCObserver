package qbsss.mcDebugging.clientruntime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class MinecraftInstaller {
    private static final String MINECRAFT_DIR = System.getProperty("user.home") + "/.minecraft";
    private static final String VERSIONS_DIR = MINECRAFT_DIR + "/versions";
    private static final String LIBRARIES_DIR = MINECRAFT_DIR + "/libraries";
    private static final String ASSETS_DIR = MINECRAFT_DIR + "/assets";
    private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void ensureVersionInstalled(String version) throws IOException {
        Path versionDir = Paths.get(VERSIONS_DIR, version);
        Path versionJson = versionDir.resolve(version + ".json");
        Path versionJar = versionDir.resolve(version + ".jar");

        if (Files.exists(versionJson) && Files.exists(versionJar)) {
            System.out.println("Minecraft " + version + " is already installed");
            return;
        }

        System.out.println("Installing Minecraft " + version + "...");
        Files.createDirectories(versionDir);

        String versionUrl = getVersionUrl(version);
        if (versionUrl == null) {
            throw new IOException("Version " + version + " not found in manifest");
        }

        JsonNode versionData = downloadJson(versionUrl);
        Files.writeString(versionJson, objectMapper.writeValueAsString(versionData));

        String clientUrl = versionData.get("downloads").get("client").get("url").asText();
        downloadFile(clientUrl, versionJar.toFile());

        downloadLibraries(versionData);
        downloadAssets(versionData);

        System.out.println("Minecraft " + version + " installed successfully");
    }

    public void prepareNatives(String version, Path nativesDir) throws IOException {
        Files.createDirectories(nativesDir);
        JsonNode versionData = readVersionData(version);
        JsonNode libraries = versionData.get("libraries");
        if (libraries == null) {
            return;
        }

        int extracted = 0;
        for (JsonNode lib : libraries) {
            if (!isLibraryAllowed(lib)) {
                continue;
            }

            JsonNode nativeArtifact = resolveNativeArtifact(lib);
            if (nativeArtifact == null) {
                continue;
            }

            String path = nativeArtifact.get("path").asText();
            Path nativeJar = Paths.get(LIBRARIES_DIR, path);
            if (!Files.exists(nativeJar)) {
                Files.createDirectories(nativeJar.getParent());
                downloadFile(nativeArtifact.get("url").asText(), nativeJar.toFile());
            }

            extracted += extractNativeLibrary(nativeJar, nativesDir);
        }

        System.out.println("Prepared " + extracted + " native files for " + version);
    }

    private JsonNode resolveNativeArtifact(JsonNode library) {
        JsonNode downloads = library.get("downloads");
        if (downloads == null || downloads.get("classifiers") == null) {
            return null;
        }

        String classifierKey = resolveNativeClassifierKey(library);
        if (classifierKey == null) {
            return null;
        }

        JsonNode classifiers = downloads.get("classifiers");
        return classifiers.get(classifierKey);
    }

    private String resolveNativeClassifierKey(JsonNode library) {
        JsonNode nativesNode = library.get("natives");
        if (nativesNode == null) {
            return null;
        }

        String osName = getOSName();
        JsonNode valueNode = nativesNode.get(osName);
        if (valueNode == null) {
            return null;
        }

        String value = valueNode.asText();
        return value.replace("${arch}", getArchitectureToken());
    }

    private int extractNativeLibrary(Path nativeJar, Path nativesDir) throws IOException {
        int extracted = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(nativeJar))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();
                if (entryName.startsWith("META-INF/") || !isNativeFile(entryName)) {
                    continue;
                }

                Path target = nativesDir.resolve(Paths.get(entryName).getFileName().toString());
                try (FileOutputStream out = new FileOutputStream(target.toFile())) {
                    zip.transferTo(out);
                }
                extracted++;
            }
        }

        return extracted;
    }

    private boolean isNativeFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".dll") || lower.endsWith(".so") || lower.endsWith(".dylib") || lower.endsWith(".jnilib");
    }

    private String getVersionUrl(String version) throws IOException {
        JsonNode manifest = downloadJson(VERSION_MANIFEST_URL);
        JsonNode versions = manifest.get("versions");

        for (JsonNode v : versions) {
            if (v.get("id").asText().equals(version)) {
                return v.get("url").asText();
            }
        }
        return null;
    }

    private void downloadLibraries(JsonNode versionData) throws IOException {
        JsonNode libraries = versionData.get("libraries");
        if (libraries == null) {
            return;
        }

        System.out.println("Downloading libraries...");
        int count = 0;

        for (JsonNode lib : libraries) {
            if (!isLibraryAllowed(lib)) {
                continue;
            }

            JsonNode downloads = lib.get("downloads");
            if (downloads == null || downloads.get("artifact") == null) {
                continue;
            }

            JsonNode artifact = downloads.get("artifact");
            String url = artifact.get("url").asText();
            String path = artifact.get("path").asText();

            Path libFile = Paths.get(LIBRARIES_DIR, path);
            if (Files.exists(libFile)) {
                continue;
            }

            Files.createDirectories(libFile.getParent());
            downloadFile(url, libFile.toFile());
            count++;
        }

        System.out.println("Downloaded " + count + " libraries");
    }

    private void downloadAssets(JsonNode versionData) throws IOException {
        JsonNode assetIndex = versionData.get("assetIndex");
        if (assetIndex == null) {
            return;
        }

        String assetIndexUrl = assetIndex.get("url").asText();
        String assetId = assetIndex.get("id").asText();

        Path indexesDir = Paths.get(ASSETS_DIR, "indexes");
        Files.createDirectories(indexesDir);

        Path indexFile = indexesDir.resolve(assetId + ".json");
        if (!Files.exists(indexFile)) {
            System.out.println("Downloading asset index...");
            JsonNode index = downloadJson(assetIndexUrl);
            Files.writeString(indexFile, objectMapper.writeValueAsString(index));
            System.out.println("Assets downloaded (index only)");
        }
    }

    private boolean isLibraryAllowed(JsonNode lib) {
        JsonNode rules = lib.get("rules");
        if (rules == null) {
            return true;
        }

        boolean allowed = false;
        for (JsonNode rule : rules) {
            if (matchesRule(rule)) {
                allowed = "allow".equals(rule.get("action").asText());
            }
        }

        return allowed;
    }

    private boolean matchesRule(JsonNode rule) {
        JsonNode featuresNode = rule.get("features");
        if (featuresNode != null && !matchesFeatures(featuresNode)) {
            return false;
        }

        JsonNode osNode = rule.get("os");
        if (osNode == null) {
            return true;
        }

        String currentName = getOSName();
        if (osNode.get("name") != null && !currentName.equals(osNode.get("name").asText())) {
            return false;
        }

        if (osNode.get("arch") != null) {
            String archRule = osNode.get("arch").asText();
            String currentArch = System.getProperty("os.arch").toLowerCase();
            if (!currentArch.contains(archRule.toLowerCase())) {
                return false;
            }
        }

        if (osNode.get("version") != null) {
            String versionPattern = osNode.get("version").asText();
            String osVersion = System.getProperty("os.version");
            if (!Pattern.compile(versionPattern).matcher(osVersion).find()) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesFeatures(JsonNode featuresNode) {
        if (featuresNode == null || !featuresNode.isObject()) {
            return true;
        }

        for (var fields = featuresNode.fields(); fields.hasNext(); ) {
            Map.Entry<String, JsonNode> feature = fields.next();
            boolean requiredValue = feature.getValue().asBoolean();
            boolean actualValue = false;
            if (requiredValue != actualValue) {
                return false;
            }
        }

        return true;
    }

    private String getOSName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac")) {
            return "osx";
        }
        if (os.contains("nux")) {
            return "linux";
        }
        return "unknown";
    }

    private String getArchitectureToken() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        return arch.contains("64") ? "64" : "32";
    }

    private JsonNode downloadJson(String urlString) throws IOException {
        URL url = new URL(urlString);
        try (InputStream in = url.openStream()) {
            return objectMapper.readTree(in);
        }
    }

    private void downloadFile(String urlString, File destination) throws IOException {
        URL url = new URL(urlString);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    public List<String> buildClasspath(String version) throws IOException {
        List<String> classpath = new ArrayList<>();

        Path clientJar = Paths.get(VERSIONS_DIR, version, version + ".jar");
        classpath.add(clientJar.toString());

        JsonNode versionData = readVersionData(version);
        JsonNode libraries = versionData.get("libraries");
        if (libraries != null) {
            for (JsonNode lib : libraries) {
                if (!isLibraryAllowed(lib)) {
                    continue;
                }

                JsonNode downloads = lib.get("downloads");
                if (downloads == null || downloads.get("artifact") == null) {
                    continue;
                }

                String path = downloads.get("artifact").get("path").asText();
                Path libFile = Paths.get(LIBRARIES_DIR, path);
                if (Files.exists(libFile)) {
                    classpath.add(libFile.toString());
                }
            }
        }

        return classpath;
    }

    public String getMainClass(String version) throws IOException {
        JsonNode versionData = readVersionData(version);
        JsonNode mainClass = versionData.get("mainClass");
        if (mainClass != null && !mainClass.isNull()) {
            return mainClass.asText();
        }
        return "net.minecraft.client.main.Main";
    }

    public String getAssetsIndexName(String version) throws IOException {
        JsonNode versionData = readVersionData(version);
        JsonNode assetIndex = versionData.get("assetIndex");
        if (assetIndex != null && assetIndex.get("id") != null) {
            return assetIndex.get("id").asText();
        }
        return version;
    }

    public List<String> getJvmArguments(String version, Path nativesDir, String classpathArg) throws IOException {
        List<String> args = new ArrayList<>();
        JsonNode versionData = readVersionData(version);
        JsonNode arguments = versionData.get("arguments");

        if (arguments != null && arguments.get("jvm") != null) {
            for (JsonNode argNode : arguments.get("jvm")) {
                if (argNode.isTextual()) {
                    args.add(replaceJvmPlaceholders(argNode.asText(), version, nativesDir, classpathArg));
                } else if (argNode.isObject() && isArgumentAllowed(argNode)) {
                    JsonNode value = argNode.get("value");
                    if (value == null) {
                        value = argNode.get("values");
                    }
                    addArgumentValues(args, value, version, nativesDir, classpathArg);
                }
            }
        }

        boolean hasNativePath = args.stream().anyMatch(a -> a.startsWith("-Djava.library.path="));
        if (!hasNativePath) {
            args.add("-Djava.library.path=" + nativesDir.toAbsolutePath());
        }

        return args;
    }

    private boolean isArgumentAllowed(JsonNode argNode) {
        JsonNode rules = argNode.get("rules");
        if (rules == null || !rules.isArray()) {
            return true;
        }

        boolean allowed = false;
        for (JsonNode rule : rules) {
            if (matchesRule(rule)) {
                allowed = "allow".equals(rule.get("action").asText());
            }
        }
        return allowed;
    }

    private void addArgumentValues(List<String> target, JsonNode valueNode, String version, Path nativesDir, String classpathArg) {
        if (valueNode == null) {
            return;
        }

        if (valueNode.isTextual()) {
            target.add(replaceJvmPlaceholders(valueNode.asText(), version, nativesDir, classpathArg));
            return;
        }

        if (valueNode.isArray()) {
            for (JsonNode item : valueNode) {
                if (item.isTextual()) {
                    target.add(replaceJvmPlaceholders(item.asText(), version, nativesDir, classpathArg));
                }
            }
        }
    }

    private String replaceJvmPlaceholders(String arg, String version, Path nativesDir, String classpathArg) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("${natives_directory}", nativesDir.toAbsolutePath().toString());
        placeholders.put("${launcher_name}", "MCObserver");
        placeholders.put("${launcher_version}", "1.0.0");
        placeholders.put("${classpath}", classpathArg);
        placeholders.put("${classpath_separator}", File.pathSeparator);
        placeholders.put("${library_directory}", LIBRARIES_DIR);
        placeholders.put("${version_name}", version);

        String result = arg;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public List<String> getGameArguments(String version) throws IOException {
        List<String> args = new ArrayList<>();
        JsonNode versionData = readVersionData(version);

        JsonNode arguments = versionData.get("arguments");
        if (arguments != null && arguments.get("game") != null) {
            for (JsonNode arg : arguments.get("game")) {
                if (arg.isTextual()) {
                    args.add(arg.asText());
                } else if (arg.isObject() && isArgumentAllowed(arg)) {
                    JsonNode value = arg.get("value");
                    if (value == null) {
                        value = arg.get("values");
                    }

                    if (value != null && value.isTextual()) {
                        args.add(value.asText());
                    } else if (value != null && value.isArray()) {
                        for (JsonNode item : value) {
                            if (item.isTextual()) {
                                args.add(item.asText());
                            }
                        }
                    }
                }
            }
            return args;
        }

        JsonNode minecraftArguments = versionData.get("minecraftArguments");
        if (minecraftArguments != null) {
            String argsString = minecraftArguments.asText();
            for (String arg : argsString.split(" ")) {
                if (!arg.isBlank()) {
                    args.add(arg);
                }
            }
        }

        return args;
    }

    private JsonNode readVersionData(String version) throws IOException {
        Path versionJson = Paths.get(VERSIONS_DIR, version, version + ".json");
        if (!Files.exists(versionJson)) {
            throw new IOException("Version metadata not found: " + versionJson);
        }
        return objectMapper.readTree(versionJson.toFile());
    }
}
