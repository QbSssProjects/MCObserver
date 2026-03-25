package qbsss.mcDebugging.agent;

import net.bytebuddy.asm.Advice;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.Severity;

import java.util.List;
import java.util.Map;

public class FileOperationAdvice {
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
        String method = methodName.toLowerCase();
        String eventType;
        if (method.contains("save")) {
            eventType = "WORLD_SAVE";
        } else if (method.contains("load")) {
            eventType = "WORLD_LOAD";
        } else if (method.startsWith("read")) {
            eventType = "FILE_READ";
        } else if (method.startsWith("write")) {
            eventType = "FILE_WRITE";
        } else {
            eventType = "IO_METHOD";
        }
        if (thrown != null) {
            eventType = eventType + "_ERROR";
        }

        EventEmitter.emitThrottled(
            "file:" + className + "#" + methodName,
            100,
            eventType,
            Category.IO,
            thrown == null ? Severity.INFO : Severity.WARN,
            duration,
            Map.of(
                "class", className,
                "method", methodName,
                "error", thrown != null ? thrown.getClass().getSimpleName() : ""
            ),
            List.of("io", "file")
        );
    }
}
