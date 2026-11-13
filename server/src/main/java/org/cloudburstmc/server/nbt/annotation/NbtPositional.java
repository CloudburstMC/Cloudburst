package org.cloudburstmc.server.nbt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Loads entries by list position instead of their {@code Slot} tags.
 * Excess entries are ignored and unlisted slots are left unchanged.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtPositional {
}
