package qbsss.mcDebugging.backend;

import org.springframework.stereotype.Service;
import qbsss.mcDebugging.core.ObservabilityEvent;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class EventFilterService {
    private volatile EventFilterConfig currentFilter = new EventFilterConfig();

    public EventFilterConfig getCurrentFilter() {
        return currentFilter;
    }

    public EventFilterConfig updateFilter(EventFilterConfig incoming) {
        EventFilterConfig filter = incoming != null ? incoming : new EventFilterConfig();
        currentFilter = filter;
        return currentFilter;
    }

    public EventFilterConfig resetFilter() {
        currentFilter = new EventFilterConfig();
        return currentFilter;
    }

    public boolean matches(ObservabilityEvent event) {
        EventFilterConfig filter = currentFilter;
        if (event == null || !filter.isEnabled()) {
            return true;
        }

        if (!filter.getCategories().isEmpty() && !filter.getCategories().contains(event.getCategory())) {
            return false;
        }

        if (!filter.getSeverities().isEmpty() && !filter.getSeverities().contains(event.getSeverity())) {
            return false;
        }

        String typeNeedle = normalize(filter.getTypeContains());
        if (!typeNeedle.isEmpty() && !normalize(event.getType()).contains(typeNeedle)) {
            return false;
        }

        String tagNeedle = normalize(filter.getTagContains());
        if (!tagNeedle.isEmpty() && !hasTag(event.getTags(), tagNeedle)) {
            return false;
        }

        return true;
    }

    private boolean hasTag(List<String> tags, String needle) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }

        return tags.stream()
            .filter(Objects::nonNull)
            .map(this::normalize)
            .anyMatch(tag -> tag.contains(needle));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
