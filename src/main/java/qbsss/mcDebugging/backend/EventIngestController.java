package qbsss.mcDebugging.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.ObservabilityEvent;
import qbsss.mcDebugging.core.Severity;
import qbsss.mcDebugging.core.ThreadInfo;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventIngestController {
    private final EventWebSocketHandler eventWebSocketHandler;
    private final EventFilterService filterService;
    private final EventStateService stateService;
    private final AtomicLong sequence = new AtomicLong(1);

    public EventIngestController(EventWebSocketHandler eventWebSocketHandler,
                                 EventFilterService filterService,
                                 EventStateService stateService) {
        this.eventWebSocketHandler = eventWebSocketHandler;
        this.filterService = filterService;
        this.stateService = stateService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingest(@RequestBody IngestEventRequest request) {
        ObservabilityEvent event = toEvent(request);
        stateService.accept(event);
        eventWebSocketHandler.sendEvent(event);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        return ResponseEntity.ok(stateService.snapshot());
    }

    @GetMapping("/filter")
    public ResponseEntity<EventFilterConfig> getFilter() {
        return ResponseEntity.ok(filterService.getCurrentFilter());
    }

    @PostMapping("/filter")
    public ResponseEntity<EventFilterConfig> setFilter(@RequestBody EventFilterConfig filterConfig) {
        return ResponseEntity.ok(filterService.updateFilter(filterConfig));
    }

    @PostMapping("/filter/reset")
    public ResponseEntity<EventFilterConfig> resetFilter() {
        return ResponseEntity.ok(filterService.resetFilter());
    }

    private ObservabilityEvent toEvent(IngestEventRequest request) {
        String id = request.id != null && !request.id.isBlank() ? request.id : UUID.randomUUID().toString();
        long timestamp = request.timestampEpochMs > 0 ? request.timestampEpochMs : System.currentTimeMillis();
        long durationNanos = Math.max(0, request.durationNanos);
        String sessionId = request.sessionId != null && !request.sessionId.isBlank() ? request.sessionId : "default";
        Category category = parseCategory(request.category);
        Severity severity = parseSeverity(request.severity);
        String type = request.type != null && !request.type.isBlank() ? request.type : "UNKNOWN_EVENT";

        ThreadInfo threadInfo = new ThreadInfo(
            request.threadName != null ? request.threadName : "unknown-thread",
            request.threadId > 0 ? request.threadId : -1
        );

        Map<String, Object> payload = request.payload != null ? request.payload : Collections.emptyMap();
        List<String> tags = request.tags != null ? request.tags : Collections.emptyList();

        return new ObservabilityEvent(
            id,
            sequence.getAndIncrement(),
            sessionId,
            type,
            category,
            severity,
            timestamp,
            durationNanos,
            threadInfo,
            payload,
            tags
        );
    }

    private Category parseCategory(String category) {
        if (category == null) {
            return Category.JVM;
        }
        try {
            return Category.valueOf(category.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Category.JVM;
        }
    }

    private Severity parseSeverity(String severity) {
        if (severity == null) {
            return Severity.INFO;
        }
        try {
            return Severity.valueOf(severity.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Severity.INFO;
        }
    }

    public static class IngestEventRequest {
        public String id;
        public String sessionId;
        public String type;
        public String category;
        public String severity;
        public long timestampEpochMs;
        public long durationNanos;
        public String threadName;
        public long threadId;
        public Map<String, Object> payload;
        public List<String> tags;
    }
}
