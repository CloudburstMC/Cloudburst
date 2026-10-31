package org.cloudburstmc.api.nbt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the NBT type a property is written as. Without it, the type follows the
 * property's Java type.
 * <p>
 * The value is converted to the declared type, for example an {@code int} annotated with
 * {@code @NbtTag(BYTE)} is written as a byte.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtTag {

    /**
     * The tag type of the property itself.
     */
    NbtTagType value() default NbtTagType.AUTO;

    /**
     * The tag type of the elements of a {@link NbtTagType#LIST} property, or of the values
     * of a {@link NbtTagType#COMPOUND} map property. It also decides the type of an empty list.
     */
    NbtTagType element() default NbtTagType.AUTO;
}
