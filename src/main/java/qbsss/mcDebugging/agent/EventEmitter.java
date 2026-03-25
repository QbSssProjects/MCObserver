package qbsss.mcDebugging.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.Severity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class EventEmitter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final LinkedBlockingQueue<byte[]> QUEUE = new LinkedBlockingQueue<>(10_000);
    private static final ConcurrentHashMap<String, Long> LAST_EVENT_BY_KEY = new ConcurrentHashMap<>();
    private static final AtomicLong DROPPED = new AtomicLong();
    private static final String DEFAULT_ENDPOINT = "http://127.0.0.1:8090/api/events/ingest";
    private static final String ENDPOINT = System.getProperty("mcobserver.backendUrl", DEFAULT_ENDPOINT);
    private static final String SESSION_ID = System.getProperty("mcobserver.sessionId", "default");

    static {
        Thread sender = new Thread(EventEmitter::senderLoop, "mcobserver-event-sender");
        sender.setDaemon(true);
        sender.start();
    }

    private EventEmitter() {
    }

    public static void emit(String type, Category category, Severity severity, long durationNanos,
                            Map<String, Object> payload, List<String> tags) {
        emitInternal(type, category, severity, durationNanos, payload, tags);
    }

    public static void emitThrottled(String key, long minIntervalMs, String type, Category category, Severity severity,
                                     long durationNanos, Map<String, Object> payload, List<String> tags) {
        long now = System.currentTimeMillis();
        Long last = LAST_EVENT_BY_KEY.putIfAbsent(key, now);
        if (last != null) {
            if (now - last < minIntervalMs) {
                return;
            }
            LAST_EVENT_BY_KEY.put(key, now);
        }
        emitInternal(type, category, severity, durationNanos, payload, tags);
    }

    private static void emitInternal(String type, Category category, Severity severity, long durationNanos,
                                     Map<String, Object> payload, List<String> tags) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("id", UUID.randomUUID().toString());
            event.put("sessionId", SESSION_ID);
            event.put("type", type);
            event.put("category", category.name());
            event.put("severity", severity.name());
            event.put("timestampEpochMs", System.currentTimeMillis());
            event.put("durationNanos", Math.max(0, durationNanos));
            event.put("threadName", Thread.currentThread().getName());
            event.put("threadId", Thread.currentThread().getId());
            event.put("payload", payload != null ? payload : Collections.emptyMap());
            event.put("tags", tags != null ? tags : Collections.emptyList());

            byte[] body = OBJECT_MAPPER.writeValueAsBytes(event);
            if (!QUEUE.offer(body)) {
                long dropped = DROPPED.incrementAndGet();
                if (dropped % 100 == 0) {
                    System.err.println("MCObserver: dropped " + dropped + " events due to full queue");
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void senderLoop() {
        while (true) {
            try {
                byte[] body = QUEUE.take();
                send(body);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private static void send(byte[] body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        conn.setConnectTimeout(1000);
        conn.setReadTimeout(1000);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setFixedLengthStreamingMode(body.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }
        conn.getResponseCode();
        conn.disconnect();
    }
}
