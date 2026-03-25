package qbsss.mcDebugging.backend;

import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.Severity;

import java.util.LinkedHashSet;
import java.util.Set;

public class EventFilterConfig {
    private boolean enabled;
    private Set<Category> categories = new LinkedHashSet<>();
    private Set<Severity> severities = new LinkedHashSet<>();
    private String typeContains = "";
    private String tagContains = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories != null ? new LinkedHashSet<>(categories) : new LinkedHashSet<>();
    }

    public Set<Severity> getSeverities() {
        return severities;
    }

    public void setSeverities(Set<Severity> severities) {
        this.severities = severities != null ? new LinkedHashSet<>(severities) : new LinkedHashSet<>();
    }

    public String getTypeContains() {
        return typeContains;
    }

    public void setTypeContains(String typeContains) {
        this.typeContains = typeContains != null ? typeContains : "";
    }

    public String getTagContains() {
        return tagContains;
    }

    public void setTagContains(String tagContains) {
        this.tagContains = tagContains != null ? tagContains : "";
    }
}
