package qbsss.mcDebugging.clientruntime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class FabricInstaller {
    private static final String MINECRAFT_DIR = System.getProperty("user.home") + "/.minecraft";
    private static final String LIBRARIES_DIR = MINECRAFT_DIR + "/libraries";
    private static final String FABRIC_META_URL = "https://meta.fabricmc.net/v2/versions";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_FABRIC_VERSION = "0.15.11";

    public void ensureFabricInstalled(String mcVersion, String fabricVersion) throws IOException {
        fabricVersion = normalizeFabricVersion(fabricVersion);
        System.out.println("Installing Fabric " + fabricVersion + " for Minecraft " + mcVersion + "...");

        // Pobierz informacje o Fabric loader
        String loaderUrl = FABRIC_META_URL + "/loader/" + mcVersion + "/" + fabricVersion;
        JsonNode loaderInfo = downloadJson(loaderUrl);

        // Pobierz biblioteki Fabric
        JsonNode libraries = loaderInfo.get("launcherMeta").get("libraries");
        if (libraries != null && libraries.get("client") != null) {
            downloadFabricLibraries(libraries.get("client"));
        }

        System.out.println("Fabric installed successfully");
    }

    private void downloadFabricLibraries(JsonNode libraries) throws IOException {
        System.out.println("Downloading Fabric libraries...");
        int count = 0;

        for (JsonNode lib : libraries) {
            String name = lib.get("name").asText();
            String url = lib.get("url").asText();

            // Konwertuj Maven coordinates na ścieżkę
            String[] parts = name.split(":");
            if (parts.length < 3) continue;

            String group = parts[0].replace('.', '/');
            String artifact = parts[1];
            String version = parts[2];
            String fileName = artifact + "-" + version + ".jar";

            Path libPath = Paths.get(LIBRARIES_DIR, group, artifact, version, fileName);

            if (Files.exists(libPath)) {
                continue; // Skip if exists
            }

            Files.createDirectories(libPath.getParent());

            String downloadUrl = url + group + "/" + artifact + "/" + version + "/" + fileName;
            try {
                downloadFile(downloadUrl, libPath.toFile());
                count++;
            } catch (IOException e) {
                System.err.println("Failed to download: " + downloadUrl);
            }
        }

        System.out.println("Downloaded " + count + " Fabric libraries");
    }

    public List<String> buildFabricClasspath(String mcVersion, String fabricVersion, List<String> vanillaClasspath) throws IOException {
        fabricVersion = normalizeFabricVersion(fabricVersion);
        List<String> classpath = new ArrayList<>(vanillaClasspath);

        // Dodaj biblioteki Fabric
        String loaderUrl = FABRIC_META_URL + "/loader/" + mcVersion + "/" + fabricVersion;
        JsonNode loaderInfo = downloadJson(loaderUrl);

        JsonNode libraries = loaderInfo.get("launcherMeta").get("libraries");
        if (libraries != null && libraries.get("client") != null) {
            for (JsonNode lib : libraries.get("client")) {
                String name = lib.get("name").asText();
                String[] parts = name.split(":");
                if (parts.length < 3) continue;

                String group = parts[0].replace('.', '/');
                String artifact = parts[1];
                String version = parts[2];
                String fileName = artifact + "-" + version + ".jar";

                Path libPath = Paths.get(LIBRARIES_DIR, group, artifact, version, fileName);
                if (Files.exists(libPath)) {
                    classpath.add(libPath.toString());
                }
            }
        }

        return classpath;
    }

    public String getFabricMainClass(String mcVersion, String fabricVersion) throws IOException {
        fabricVersion = normalizeFabricVersion(fabricVersion);
        String loaderUrl = FABRIC_META_URL + "/loader/" + mcVersion + "/" + fabricVersion;
        JsonNode loaderInfo = downloadJson(loaderUrl);

        JsonNode mainClass = loaderInfo.get("launcherMeta").get("mainClass");
        if (mainClass != null && mainClass.get("client") != null) {
            return mainClass.get("client").asText();
        }

        return "net.fabricmc.loader.impl.launch.knot.KnotClient";
    }

    private String normalizeFabricVersion(String fabricVersion) {
        if (fabricVersion == null || fabricVersion.isBlank()) {
            return DEFAULT_FABRIC_VERSION;
        }
        return fabricVersion;
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
}
