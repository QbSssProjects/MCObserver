package qbsss.mcDebugging.agent;

import net.bytebuddy.asm.Advice;
import qbsss.mcDebugging.core.Category;
import qbsss.mcDebugging.core.Severity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHealthAdvice {
    private static final ConcurrentHashMap<Integer, Double> LAST_HEALTH = new ConcurrentHashMap<>();

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static EnterData onEnter(@Advice.This(optional = true) Object self) {
        double before = readHealth(self);
        return new EnterData(System.nanoTime(), before);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(@Advice.Enter EnterData enter,
                              @Advice.This(optional = true) Object self,
                              @Advice.AllArguments Object[] args,
                              @Advice.Origin("#m") String methodName,
                              @Advice.Thrown Throwable thrown) {
        if (thrown != null) {
            return;
        }

        int playerId = self != null ? System.identityHashCode(self) : -1;
        double after = inferHealthAfter(self, args, methodName, enter.beforeHealth);
        Double previous = LAST_HEALTH.put(playerId, after);
        double before = previous != null ? previous : enter.beforeHealth;
        double delta = after - before;

        String eventType;
        if (delta > 0.0001d) {
            eventType = "HEALTH_INCREASED";
        } else if (delta < -0.0001d) {
            eventType = "HEALTH_DECREASED";
        } else {
            eventType = "HEALTH_CHANGED";
        }

        EventEmitter.emit(
            eventType,
            Category.MINECRAFT,
            Severity.INFO,
            System.nanoTime() - enter.startNanos,
            Map.of(
                "method", methodName,
                "healthCurrent", after,
                "healthDelta", delta,
                "playerId", playerId
            ),
            List.of("minecraft", "player", "health")
        );
    }

    private static double inferHealthAfter(Object self, Object[] args, String methodName, double fallback) {
        double reflected = readHealth(self);
        if (!Double.isNaN(reflected)) {
            return reflected;
        }

        if (args != null && args.length > 0 && args[0] instanceof Number number) {
            double val = number.doubleValue();
            if ("heal".equalsIgnoreCase(methodName)) {
                return fallback + val;
            }
            if ("hurt".equalsIgnoreCase(methodName) || "damage".equalsIgnoreCase(methodName)) {
                return Math.max(0, fallback - val);
            }
            return val;
        }
        return fallback;
    }

    private static double readHealth(Object self) {
        if (self == null) {
            return Double.NaN;
        }
        try {
            Method method = self.getClass().getMethod("getHealth");
            Object value = method.invoke(self);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception ignored) {
        }
        return Double.NaN;
    }

    public record EnterData(long startNanos, double beforeHealth) {
    }
}
