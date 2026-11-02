package org.cloudburstmc.server.event.firehandler;

import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.EventFireHandler;
import org.cloudburstmc.api.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ReflectionEventFireHandler implements EventFireHandler {

    private static final long LONG_RUNNING_EVENT_TIME = TimeUnit.MILLISECONDS.toNanos(5);
    private final List<EventFireHandler.ListenerMethod> methods = new ArrayList<>();

    public ReflectionEventFireHandler(Collection<? extends EventFireHandler.ListenerMethod> methods) {
        this.methods.addAll(methods);
    }

    @Override
    public void fire(Event event) {
        long start = System.nanoTime();
        for (EventFireHandler.ListenerMethod method : methods) {
            try {
                method.run(event);
            } catch (Throwable e) {
                log.error("Exception occurred while executing method {} for {}", method, event, e);
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
        return List.copyOf(methods);
    }

    public static class ListenerMethod implements EventFireHandler.ListenerMethod {

        private final Object listener;
        private final Method method;
        private final Listener configuration;

        public ListenerMethod(Object listener, Method method) {
            this.listener = Objects.requireNonNull(listener, "listener");
            this.method = Objects.requireNonNull(method, "method");
            this.configuration = Objects.requireNonNull(method.getAnnotation(Listener.class), "listener annotation");
            this.method.setAccessible(true);
        }

        @Override
        public void run(Event event) throws InvocationTargetException, IllegalAccessException {
            if (this.configuration.ignoreCancelled() && event instanceof Cancellable && event.isCancelled()) {
                return;
            }
            method.invoke(listener, event);
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
        public int compareTo(EventFireHandler.@NonNull ListenerMethod o) {
            Listener thatListener = o.getMethod().getAnnotation(Listener.class);
            if (thatListener == null) {
                return 1;
            }

            return Integer.compare(this.configuration.priority().ordinal(), thatListener.priority().ordinal());
        }
    }
}
