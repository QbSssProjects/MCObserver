package qbsss.mcDebugging.clientruntime;

public class MinecraftVersion {
    private final String version;
    private final String releaseType; // release, snapshot
    private final boolean supportsFabric;
    private final boolean supportsForge;

    public MinecraftVersion(String version, String releaseType, boolean supportsFabric, boolean supportsForge) {
        this.version = version;
        this.releaseType = releaseType;
        this.supportsFabric = supportsFabric;
        this.supportsForge = supportsForge;
    }

    public String getVersion() {
        return version;
    }

    public String getReleaseType() {
        return releaseType;
    }

    public boolean supportsFabric() {
        return supportsFabric;
    }

    public boolean supportsForge() {
        return supportsForge;
    }
}
