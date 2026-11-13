package org.cloudburstmc.server.nbt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supplies the slot count when loading an item container property with no existing container.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtContainerSize {

    /**
     * The slot count of containers created for the property.
     */
    int value();
}
