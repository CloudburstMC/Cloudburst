package org.cloudburstmc.api.nbt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Writes a block state as its bare {@code {name, states}} compound, as falling blocks
 * are stored today, instead of the versioned serialized form other block-state
 * properties use. Reading accepts either shape.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtShortBlockState {
}
