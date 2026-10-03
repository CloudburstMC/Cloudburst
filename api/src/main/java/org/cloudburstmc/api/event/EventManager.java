package org.cloudburstmc.api.event;

import java.util.Collection;

/**
 * Manages plugin-owned event listeners and dispatches events.
 * Removing listeners does not interrupt a dispatch already in progress.
 */
public interface EventManager {

    /**
     * Registers methods declared by the listener's class and annotated with {@link Listener}.
     * Each method must accept exactly one parameter of type {@link Event} or a subclass.
     *
     * @param plugin   the registered plugin instance that owns the listeners
     * @param listener the listener object
     * @throws IllegalArgumentException if the plugin is not registered or a listener method has an invalid parameter
     */
    void registerListeners(Object plugin, Object listener);

    /**
     * Invokes matching listeners on the calling thread before returning.
     * Listeners for a base event type also receive its subclasses.
     * Listeners run in priority order from {@link EventPriority#LOWEST} to {@link EventPriority#MONITOR}.
     * Listener failures are logged without preventing the remaining listeners from running.
     * <p>
     * The caller must follow the event's threading contract.
     *
     * @param event the event to fire
     */
    void fire(Event event);

    /**
     * Removes the listener object's registrations across all plugins.
     * An unregistered object has no effect.
     *
     * @param listener the listener object to remove
     */
    void deregisterListener(Object listener);

    /**
     * Removes all listeners owned by the plugin.
     *
     * @param plugin the registered plugin instance whose listeners are removed
     * @throws IllegalArgumentException if the plugin is not registered
     */
    void deregisterAllListeners(Object plugin);

    /**
     * Removes registrations for the supplied listener objects across all plugins.
     * An empty collection or unregistered objects have no effect.
     *
     * @param listeners the listener objects to remove
     */
    void deregisterListeners(Collection<Object> listeners);
}
