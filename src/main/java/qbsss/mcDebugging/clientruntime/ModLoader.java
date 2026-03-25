package qbsss.mcDebugging.clientruntime;

public enum ModLoader {
    VANILLA("Vanilla", ""),
    FABRIC("Fabric", "fabric-loader"),
    FORGE("Forge", "forge");

    private final String displayName;
    private final String identifier;

    ModLoader(String displayName, String identifier) {
        this.displayName = displayName;
        this.identifier = identifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdentifier() {
        return identifier;
    }
}
