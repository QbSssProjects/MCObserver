package qbsss.mcDebugging.backend;

import org.springframework.stereotype.Service;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.ObservabilityEvent;
import qbsss.mcDebugging.core.Severity;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EventStateService {
    private final AtomicLong totalEvents = new AtomicLong();
    private final AtomicLong inventoryChanges = new AtomicLong();
    private final AtomicLong fileReads = new AtomicLong();
    private final AtomicLong fileWrites = new AtomicLong();
    private final AtomicLong worldSaves = new AtomicLong();
    private final AtomicLong worldLoads = new AtomicLong();

    private final Map<String, AtomicLong> typeCounts = new ConcurrentHashMap<>();
    private final Map<Category, AtomicLong> categoryCounts = new ConcurrentHashMap<>();
    private final Map<Severity, AtomicLong> severityCounts = new ConcurrentHashMap<>();

    private volatile Double healthCurrent;
    private volatile Double healthDeltaLast;
    private volatile long lastEventTs;
    private volatile String lastEventType = "";

    public void accept(ObservabilityEvent event) {
        if (event == null) {
            return;
        }

        totalEvents.incrementAndGet();
        lastEventTs = event.getTimestampEpochMs();
        lastEventType = event.getType() != null ? event.getType() : "";

        if (event.getType() != null) {
            typeCounts.computeIfAbsent(event.getType(), k -> new AtomicLong()).incrementAndGet();
        }

        if (event.getCategory() != null) {
            categoryCounts.computeIfAbsent(event.getCategory(), k -> new AtomicLong()).incrementAndGet();
        }

        if (event.getSeverity() != null) {
            severityCounts.computeIfAbsent(event.getSeverity(), k -> new AtomicLong()).incrementAndGet();
        }

        String type = event.getType() != null ? event.getType() : "";
        switch (type) {
            case "INVENTORY_CHANGED" -> inventoryChanges.incrementAndGet();
            case "FILE_READ" -> fileReads.incrementAndGet();
            case "FILE_WRITE" -> fileWrites.incrementAndGet();
            case "WORLD_SAVE" -> worldSaves.incrementAndGet();
            case "WORLD_LOAD" -> worldLoads.incrementAndGet();
            case "HEALTH_INCREASED", "HEALTH_DECREASED", "HEALTH_CHANGED" -> updateHealth(event);
            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateHealth(ObservabilityEvent event) {
        if (event.getPayload() == null) {
            return;
        }
        Object current = event.getPayload().get("healthCurrent");
        Object delta = event.getPayload().get("healthDelta");
        if (current instanceof Number number) {
            healthCurrent = number.doubleValue();
        }
        if (delta instanceof Number number) {
            healthDeltaLast = number.doubleValue();
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("totalEvents", totalEvents.get());
        snapshot.put("inventoryChanges", inventoryChanges.get());
        snapshot.put("fileReads", fileReads.get());
        snapshot.put("fileWrites", fileWrites.get());
        snapshot.put("worldSaves", worldSaves.get());
        snapshot.put("worldLoads", worldLoads.get());
        snapshot.put("healthCurrent", healthCurrent);
        snapshot.put("healthDeltaLast", healthDeltaLast);
        snapshot.put("lastEventTs", lastEventTs);
        snapshot.put("lastEventType", lastEventType);
        snapshot.put("typeCounts", flatten(typeCounts));
        snapshot.put("categoryCounts", flatten(categoryCounts));
        snapshot.put("severityCounts", flatten(severityCounts));
        return snapshot;
    }

    private <K> Map<String, Long> flatten(Map<K, AtomicLong> source) {
        Map<String, Long> result = new ConcurrentHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value.get()));
        return result;
    }
}
