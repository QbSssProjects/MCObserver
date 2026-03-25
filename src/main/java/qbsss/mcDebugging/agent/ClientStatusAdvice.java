package qbsss.mcDebugging.agent;

import net.bytebuddy.asm.Advice;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.Severity;

import java.util.List;
import java.util.Map;

public class ClientStatusAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long onEnter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(@Advice.Enter long startNanos,
                              @Advice.Origin("#m") String methodName,
                              @Advice.Thrown Throwable thrown) {
        long duration = System.nanoTime() - startNanos;
        Runtime runtime = Runtime.getRuntime();
        long usedMem = runtime.totalMemory() - runtime.freeMemory();

        EventEmitter.emitThrottled(
            "status:" + methodName,
            1000,
            thrown == null ? "CLIENT_STATUS" : "CLIENT_STATUS_ERROR",
            Category.MINECRAFT,
            thrown == null ? Severity.DEBUG : Severity.WARN,
            duration,
            Map.of(
                "method", methodName,
                "usedMemoryBytes", usedMem,
                "maxMemoryBytes", runtime.maxMemory(),
                "availableProcessors", runtime.availableProcessors(),
                "error", thrown != null ? thrown.getClass().getSimpleName() : ""
            ),
            List.of("minecraft", "status", "client")
        );
    }
}
