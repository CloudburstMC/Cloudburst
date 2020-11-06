package org.cloudburstmc.server.event;

import co.aikar.timings.Timings;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.server.event.firehandler.ReflectionEventFireHandler;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;

@Log4j2
@ParametersAreNonnullByDefault
@Singleton
public class CloudEventManager implements EventManager {

    private final Map<PluginContainer, Set<Object>> listenersByPlugin = new IdentityHashMap<>();
    private final Object registerLock = new Object();
    private volatile Map<Class<? extends Event>, EventFireHandler> eventHandlers = Collections.emptyMap();

    private final PluginManager pluginManager;

    @Inject
    public CloudEventManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public void registerListeners(Object plugin, Object listener) {
        PluginContainer container = ensurePlugin(plugin);
        checkNotNull(listener, "listener");

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
                    log.warn("{} registered deprecated event {} in {}", container.getDescription().getName(),
                            eventClass.getSimpleName(), listener.getClass().getSimpleName());
                }

                method.setAccessible(true);
                validListener = true;
            }
        }

        if (validListener) {
            synchronized (registerLock) {
                listenersByPlugin.computeIfAbsent(container, k -> new ReferenceOpenHashSet<>()).add(listener);
                bakeHandlers();
            }
        }
    }

    @Override
    public void fire(Event event) {
        checkNotNull(event, "event");
        EventFireHandler handler = eventHandlers.get(event.getClass());
        if (handler != null) {
            handler.fire(event);
        }
    }

    @Override
    public void deregisterListener(Object listener) {
        checkNotNull(listener, "listener");
        synchronized (registerLock) {
            for (Set<Object> listeners : listenersByPlugin.values()) {
                listeners.remove(listener);
            }
            bakeHandlers();
        }
    }

    @Override
    public void deregisterAllListeners(Object plugin) {
        PluginContainer container = ensurePlugin(plugin);

        synchronized (registerLock) {
            Set<Object> listeners = listenersByPlugin.remove(container);
            if (listeners != null) {
                bakeHandlers();
            }
        }
    }

    @Override
    public void deregisterListeners(Collection<Object> listeners) {
        checkNotNull(listeners, "listeners");
        synchronized (registerLock) {
            if (listeners.size() > 0) {
                for (Set<Object> pluginListeners : this.listenersByPlugin.values()) {
                    pluginListeners.removeAll(listeners);
                }
                bakeHandlers();
            }
        }
    }

    @Nonnull
    private PluginContainer ensurePlugin(Object plugin) {
        checkNotNull(plugin, "plugin");
        return this.pluginManager.fromInstance(plugin).orElseThrow(() ->
                new IllegalArgumentException("Object is not a registered plugin"));
    }

    public List<EventFireHandler.ListenerMethod> getEventListenerMethods(Class<? extends Event> eventClass) {
        checkNotNull(eventClass, "eventClass");
        return eventHandlers.get(eventClass).getMethods();
    }

    @SuppressWarnings("unchecked")
    private void bakeHandlers() {
        Map<Class<? extends Event>, List<ReflectionEventFireHandler.ListenerMethod>> listenerMap = new HashMap<>();

        for (Entry<PluginContainer, Set<Object>> entry : listenersByPlugin.entrySet()) {
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
