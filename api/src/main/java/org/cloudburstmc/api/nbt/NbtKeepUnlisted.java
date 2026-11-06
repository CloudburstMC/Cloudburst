package org.cloudburstmc.api.nbt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reads a sparse item container without clearing it first: entries bind by their
 * {@code Slot} tag and unlisted slots keep their current item, exactly as player
 * inventories load today. A short or empty list leaves the remaining slots untouched
 * instead of clearing them.
 * <p>
 * Writing is unchanged: only non-empty slots are written, each with its {@code Slot}
 * byte, as {@link NbtSkipEmpty} stores them.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtKeepUnlisted {
}
