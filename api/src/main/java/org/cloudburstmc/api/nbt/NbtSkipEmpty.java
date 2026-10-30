package org.cloudburstmc.api.nbt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stores an item container sparsely: only non-empty slots are written, each with its
 * {@code Slot} byte, exactly as chest boats are stored today. Block containers keep
 * the default dense shape, which writes every slot, air for empty.
 * <p>
 * Reading is unchanged: entries bind by their {@code Slot} tag and a read fills the
 * container in place.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtSkipEmpty {
}
