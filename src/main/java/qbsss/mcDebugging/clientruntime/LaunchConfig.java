package qbsss.mcDebugging.clientruntime;

import java.util.ArrayList;
import java.util.List;

public class LaunchConfig {
    private String instanceName;
    private String minecraftVersion;
    private ModLoader modLoader;
    private String modLoaderVersion;
    private List<String> modPaths;
    private String javaPath;
    private int maxMemoryMB;
    private List<String> jvmArgs;
    private boolean attachAgent;

    public LaunchConfig() {
        this.instanceName = "default";
        this.modPaths = new ArrayList<>();
        this.jvmArgs = new ArrayList<>();
        this.maxMemoryMB = 4096;
        this.attachAgent = true;
    }

    // Getters and setters
    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public ModLoader getModLoader() {
        return modLoader;
    }

    public void setModLoader(ModLoader modLoader) {
        this.modLoader = modLoader;
    }

    public String getModLoaderVersion() {
        return modLoaderVersion;
    }

    public void setModLoaderVersion(String modLoaderVersion) {
        this.modLoaderVersion = modLoaderVersion;
    }

    public List<String> getModPaths() {
        return modPaths;
    }

    public void setModPaths(List<String> modPaths) {
        this.modPaths = modPaths;
    }

    public void addMod(String modPath) {
        this.modPaths.add(modPath);
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public int getMaxMemoryMB() {
        return maxMemoryMB;
    }

    public void setMaxMemoryMB(int maxMemoryMB) {
        this.maxMemoryMB = maxMemoryMB;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public boolean isAttachAgent() {
        return attachAgent;
    }

    public void setAttachAgent(boolean attachAgent) {
        this.attachAgent = attachAgent;
    }
}
