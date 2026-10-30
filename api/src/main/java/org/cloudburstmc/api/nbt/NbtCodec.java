package org.cloudburstmc.api.nbt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stores a property with a codec instead of the encoding its type has by default, in both
 * directions. It applies to NBT only; other mappers of the same object ignore it.
 * <p>
 * The codec decides the written NBT type, so an {@link NbtTag} on the same property has
 * no effect.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtCodec {

    /**
     * The codec class. It needs a no-arg constructor; one instance per class is shared.
     */
    Class<? extends NbtValueCodec<?>> value();
}
