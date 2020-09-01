package org.cloudburstmc.server.plugin.event;

import co.aikar.timings.Timings;
import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.event.EventFireHandler;
import org.cloudburstmc.server.event.EventManager;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.plugin.PluginContainer;
import org.cloudburstmc.server.plugin.event.firehandler.ReflectionEventFireHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

@Log4j2
@ParametersAreNonnullByDefault
@Singleton
public class CloudEventManager implements EventManager {

    private final Map<PluginContainer, List<Object>> listenersByPlugin = new HashMap<>();
    private final Deque<Object> listeners = new ArrayDeque<>();
    private final Object registerLock = new Object();
    private volatile Map<Class<? extends Event>, EventFireHandler> eventHandlers = Collections.emptyMap();

    private final Server server;

    @Inject
    public CloudEventManager(Server server) {
        this.server = server;
    }

    @Override
    public void registerListeners(Object plugin, Object listener) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(listener, "listener");

        val container = this.server.getPluginManager().fromInstance(plugin).orElseThrow(() -> new IllegalArgumentException("Unknown plugin: " + plugin));

        // Verify that all listeners are valid.
        boolean validListener = false;
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Listener.class)) {
                if (method.isBridge() || method.isSynthetic()) {
                    continue;
                }
                if (method.getParameterCount() != 1) {
                    throw new IllegalArgumentException("Method " + method.getName() + " in " + listener + " does not accept only one parameter.");
                }

                Class<?> eventClass = method.getParameterTypes()[0];

                if (!Event.class.isAssignableFrom(eventClass)) {
                    throw new IllegalArgumentException("Method " + method.getName() + " in " + listener + " does not accept a subclass of Event.");
                }

                if (eventClass.getAnnotation(Deprecated.class) != null) {
                    log.warn("{} registered deprecated event {} in {}", container.getName(),
                            eventClass.getSimpleName(), listener.getClass().getSimpleName());
                }

                method.setAccessible(true);
                validListener = true;
            }
        }

        if (validListener) {
            synchronized (registerLock) {
                listenersByPlugin.computeIfAbsent(container, k -> new ArrayList<>()).add(listener);
                listeners.add(listener);
                bakeHandlers();
            }
        }
    }

    @Override
    public void fire(Event event) {
        Preconditions.checkNotNull(event, "event");
        EventFireHandler handler = eventHandlers.get(event.getClass());
        if (handler != null) {
            handler.fire(event);
        }
    }

    @Override
    public void deregisterListener(Object listener) {
        Preconditions.checkNotNull(listener, "listener");
        synchronized (registerLock) {
            for (List<Object> listeners : listenersByPlugin.values()) {
                listeners.remove(listener);
            }
            listeners.remove(listener);
            bakeHandlers();
        }
    }

    @Override
    public void deregisterAllListeners(Object plugin) {
        Preconditions.checkNotNull(plugin, "plugin");
        synchronized (registerLock) {
            PluginContainer container;
            if (plugin instanceof PluginContainer) {
                container = (PluginContainer) plugin;
            } else {
                container = server.getPluginManager().fromInstance(plugin).orElse(null);
            }

            List<Object> listeners = listenersByPlugin.remove(container);
            if (listeners != null) {
                this.listeners.removeAll(listeners);
                bakeHandlers();
            }
        }
    }

    @Override
    public void deregisterListeners(Collection<Object> listeners) {
        Preconditions.checkNotNull(listeners, "listeners");
        synchronized (registerLock) {
            if (listeners.size() > 0) {
                this.listeners.removeAll(listeners);
                bakeHandlers();
            }
        }
    }

    public List<EventFireHandler.ListenerMethod> getEventListenerMethods(Class<? extends Event> eventClass) {
        Preconditions.checkNotNull(eventClass, "eventClass");
        return eventHandlers.get(eventClass).getMethods();
    }

    @SuppressWarnings("unchecked")
    private void bakeHandlers() {
        Map<Class<? extends Event>, List<ReflectionEventFireHandler.ListenerMethod>> listenerMap = new HashMap<>();

        for (Entry<PluginContainer, List<Object>> entry : listenersByPlugin.entrySet()) {
            val container = entry.getKey();

            for (Object listener : entry.getValue()) {
                for (Method method : listener.getClass().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Listener.class)) {
                        val event = (Class<? extends Event>) method.getParameterTypes()[0];
                        listenerMap.computeIfAbsent(event, (k) -> new ArrayList<>())
                                .add(new ReflectionEventFireHandler.ListenerMethod(listener, method, Timings.getPluginEventTiming(event, listener, method, container)));
                    }
                }
            }
        }

        for (List<ReflectionEventFireHandler.ListenerMethod> methods : listenerMap.values()) {
            Collections.sort(methods);
        }

        Map<Class<? extends Event>, EventFireHandler> handlerMap = new HashMap<>();
        for (Map.Entry<Class<? extends Event>, List<ReflectionEventFireHandler.ListenerMethod>> entry : listenerMap.entrySet()) {
            handlerMap.put(entry.getKey(), new ReflectionEventFireHandler(entry.getValue()));
        }
        this.eventHandlers = Collections.unmodifiableMap(handlerMap);
    }
}
