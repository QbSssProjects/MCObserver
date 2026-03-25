package qbsss.mcDebugging.agent;

import net.bytebuddy.asm.Advice;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.Severity;

import java.util.List;
import java.util.Map;

public class PlayerMethodAdvice {
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
        String eventType = thrown == null ? "PLAYER_METHOD" : "PLAYER_METHOD_ERROR";
        Severity severity = thrown == null ? Severity.INFO : Severity.WARN;

        EventEmitter.emitThrottled(
            "player:" + className + "#" + methodName,
            120,
            eventType,
            Category.MINECRAFT,
            severity,
            duration,
            Map.of(
                "class", className,
                "method", methodName,
                "error", thrown != null ? thrown.getClass().getSimpleName() : ""
            ),
            List.of("minecraft", "player")
        );
    }
}
