package org.cloudburstmc.server.event;

import org.cloudburstmc.api.event.Event;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

/**
 * This class manages event listeners and fires events.
 */
@ParametersAreNonnullByDefault
public interface EventManager {

    /**
     * Registers an {@link Object} with event listeners.
     *
     * @param plugin   the plugin associated
     * @param listener the listener object
     */
    void registerListeners(Object plugin, Object listener);

    /**
     * Fires an event.
     *
     * @param event the event to fire
     */
    void fire(Event event);

    /**
     * Unregisters an object's event listeners.
     *
     * @param listener the object to deregister
     */
    void deregisterListener(Object listener);

    /**
     * Unregisters a plugin's event listeners.
     *
     * @param plugin the plugin to deregister
     */
    void deregisterAllListeners(Object plugin);

    void deregisterListeners(Collection<Object> listeners);
}