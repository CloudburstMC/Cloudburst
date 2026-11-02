package org.cloudburstmc.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method accepting one event parameter as a listener.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {

    /**
     * Returns the invocation priority. Lower priorities run before higher priorities.
     *
     * @return the listener priority
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Returns whether canceled events skip this listener.
     * Events that cannot be canceled are always delivered.
     *
     * @return whether to skip canceled events
     */
    boolean ignoreCancelled() default false;
}
