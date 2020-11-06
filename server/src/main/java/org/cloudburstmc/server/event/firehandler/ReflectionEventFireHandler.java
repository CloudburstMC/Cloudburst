package org.cloudburstmc.server.event.firehandler;

import co.aikar.timings.Timing;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.event.EventFireHandler;
import org.cloudburstmc.server.event.Listener;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ReflectionEventFireHandler implements EventFireHandler {
    private static final long LONG_RUNNING_EVENT_TIME = TimeUnit.MILLISECONDS.toNanos(5);
    private final List<ListenerMethod> methods = new ArrayList<>();

    public ReflectionEventFireHandler(Collection<ListenerMethod> methods) {
        this.methods.addAll(methods);
    }

    @Override
    public void fire(Event event) {
        long start = System.nanoTime();
        for (ListenerMethod method : methods) {
            try {
                method.run(event);
            } catch (Throwable e) {
                log.error("Exception occurred while executing method " + method + " for " + event, e);
            }
        }
        long differenceTaken = System.nanoTime() - start;
        if (differenceTaken >= LONG_RUNNING_EVENT_TIME) {
            log.warn("Event {} took {} ms to fire", event, BigDecimal.valueOf(differenceTaken)
                    .divide(new BigDecimal("1000000"), RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP));
        }
    }

    @Override
    public List<EventFireHandler.ListenerMethod> getMethods() {
        return Collections.unmodifiableList(new ArrayList<>(methods));
    }

    @RequiredArgsConstructor
    public static class ListenerMethod implements EventFireHandler.ListenerMethod {

        private final Object listener;
        private final Method method;
        private final Timing timing;

        public void run(Event event) throws InvocationTargetException, IllegalAccessException {
            try (Timing t = this.timing.startTiming()) {
                method.invoke(listener, event);
            }
        }

        @Override
        public String toString() {
            return listener.getClass().getName() + "#" + method.getName();
        }

        @Override
        public Object getListener() {
            return listener;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public int compareTo(@Nonnull EventFireHandler.ListenerMethod o) {
            Listener thisListener = getMethod().getAnnotation(Listener.class);
            if (listener == null) {
                return -1;
            }

            Listener thatListener = o.getMethod().getAnnotation(Listener.class);
            if (thatListener == null) {
                return 1;
            }

            return Integer.compare(thisListener.priority().ordinal(), thatListener.priority().ordinal());
        }
    }
}
