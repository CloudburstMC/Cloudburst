package org.cloudburstmc.api.event;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Dispatches events to registered listener methods.
 */
public interface EventFireHandler {

    /**
     * @param event the event to dispatch
     */
    void fire(Event event);

    /**
     * @return the registered listener methods
     */
    List<ListenerMethod> getMethods();

    /**
     * Represents a registered event listener method.
     */
    interface ListenerMethod extends Comparable<ListenerMethod> {

        /**
         * @return the listener instance
         */
        Object getListener();

        /**
         * @return the listener method
         */
        Method getMethod();

        /**
         * @param event the event to pass to the listener
         * @throws Exception if the listener cannot be invoked
         */
        void run(Event event) throws Exception;
    }
}
