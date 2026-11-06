package org.cloudburstmc.api.nbt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stores an item container positionally: the first list entry fills slot 0, the
 * next fills slot 1, and so on, ignoring any {@code Slot} tag. Entries past the
 * container's size are ignored, and a short or empty list leaves the remaining
 * slots untouched instead of clearing them, exactly like the previous load path.
 * Writing is unchanged: every slot is written, air for empty, with a {@code Slot} byte.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtPositional {
}
