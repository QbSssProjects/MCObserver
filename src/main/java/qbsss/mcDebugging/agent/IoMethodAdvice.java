package qbsss.mcDebugging.agent;

import net.bytebuddy.asm.Advice;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.Severity;

import java.util.List;
import java.util.Map;

public class IoMethodAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long onEnter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(@Advice.Enter long startNanos,
                              @Advice.Origin("#t") String className,
                              @Advice.Origin("#m") String methodName,
                              @Advice.Thrown Throwable thrown) {
        long duration = System.nanoTime() - startNanos;
        String eventType = thrown == null ? "IO_METHOD" : "IO_METHOD_ERROR";
        Severity severity = thrown == null ? Severity.DEBUG : Severity.WARN;

        EventEmitter.emitThrottled(
            "io:" + className + "#" + methodName,
            150,
            eventType,
            Category.IO,
            severity,
            duration,
            Map.of(
                "class", className,
                "method", methodName,
                "error", thrown != null ? thrown.getClass().getSimpleName() : ""
            ),
            List.of("io")
        );
    }
}
