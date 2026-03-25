package qbsss.mcDebugging.agent;

import net.bytebuddy.asm.Advice;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.Severity;

import java.util.List;
import java.util.Map;

public class InventoryAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long onEnter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(@Advice.Enter long startNanos,
                              @Advice.Origin("#m") String methodName,
                              @Advice.This(optional = true) Object self,
                              @Advice.AllArguments Object[] args,
                              @Advice.Thrown Throwable thrown) {
        if (thrown != null) {
            return;
        }

        int ownerId = self != null ? System.identityHashCode(self) : -1;
        String method = methodName.toLowerCase();
        String action;
        if (method.contains("add") || method.contains("insert") || method.contains("pickup")) {
            action = "add";
        } else if (method.contains("remove") || method.contains("drop") || method.contains("clear")) {
            action = "remove";
        } else if (method.contains("swap") || method.contains("setitem")) {
            action = "update";
        } else {
            action = "change";
        }

        EventEmitter.emitThrottled(
            "inv:" + ownerId + ":" + methodName,
            80,
            "INVENTORY_CHANGED",
            Category.MINECRAFT,
            Severity.INFO,
            System.nanoTime() - startNanos,
            Map.of(
                "action", action,
                "method", methodName,
                "argsCount", args != null ? args.length : 0,
                "ownerId", ownerId
            ),
            List.of("minecraft", "player", "inventory")
        );
    }
}
