package qbsss.mcDebugging.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import qbsss.mcDebugging.clientruntime.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/launcher")
@CrossOrigin(origins = "*")
public class LauncherController {

    @Autowired
    private VersionService versionService;

    @Autowired
    private ClientLauncher clientLauncher;

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/.minecraft/mcobserver/uploaded-mods";

    public LauncherController() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/versions")
    public ResponseEntity<List<MinecraftVersion>> getVersions() {
        return ResponseEntity.ok(versionService.getAllVersions());
    }

    @GetMapping("/modloaders/{version}")
    public ResponseEntity<List<String>> getSupportedModLoaders(@PathVariable String version) {
        return ResponseEntity.ok(versionService.getSupportedModLoaders(version));
    }

    @GetMapping("/fabric-versions")
    public ResponseEntity<List<String>> getFabricVersions() {
        return ResponseEntity.ok(versionService.getFabricLoaderVersions());
    }

    @GetMapping("/forge-versions/{mcVersion}")
    public ResponseEntity<List<String>> getForgeVersions(@PathVariable String mcVersion) {
        return ResponseEntity.ok(versionService.getForgeVersionsForMC(mcVersion));
    }

    @PostMapping("/launch")
    public ResponseEntity<Map<String, String>> launchClient(@RequestBody LaunchConfig config) {
        Map<String, String> response = new HashMap<>();

        try {
            clientLauncher.launchClient(config);
            response.put("status", "success");
            response.put("message", "Minecraft launched successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to launch: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopClient() {
        Map<String, String> response = new HashMap<>();
        clientLauncher.stopClient();
        response.put("status", "success");
        response.put("message", "Client stopped");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("running", clientLauncher.isRunning());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/upload-mod")
    public ResponseEntity<Map<String, String>> uploadMod(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("status", "error");
            response.put("message", "No file uploaded");
            return ResponseEntity.badRequest().body(response);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".jar")) {
            response.put("status", "error");
            response.put("message", "Only .jar files are allowed");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Path targetPath = Paths.get(UPLOAD_DIR, filename);
            file.transferTo(targetPath.toFile());

            response.put("status", "success");
            response.put("message", "Mod uploaded successfully");
            response.put("path", targetPath.toString());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("status", "error");
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/uploaded-mods")
    public ResponseEntity<List<String>> getUploadedMods() {
        try {
            List<String> mods = Files.list(Paths.get(UPLOAD_DIR))
                .filter(path -> path.toString().endsWith(".jar"))
                .map(path -> path.getFileName().toString())
                .toList();
            return ResponseEntity.ok(mods);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @DeleteMapping("/uploaded-mods/{filename}")
    public ResponseEntity<Map<String, String>> deleteUploadedMod(@PathVariable String filename) {
        Map<String, String> response = new HashMap<>();

        try {
            Path filePath = Paths.get(UPLOAD_DIR, filename);
            if (Files.deleteIfExists(filePath)) {
                response.put("status", "success");
                response.put("message", "Mod deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "File not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (IOException e) {
            response.put("status", "error");
            response.put("message", "Delete failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
