package org.cloudburstmc.api.nbt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the slot count of an item container property, so a read with no existing
 * container instance can create one of the right size.
 * <p>
 * The property is still written and filled by the container serializer the server
 * registers; this annotation only supplies the size for
 * {@link org.cloudburstmc.api.inventory.ItemContainers#detached(int)} when there is
 * nothing to fill in place. A container property without it fails its read when its
 * current value is {@code null}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtContainerSize {

    /**
     * The slot count of containers created for the property.
     */
    int value();
}
