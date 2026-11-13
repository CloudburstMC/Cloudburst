package org.cloudburstmc.server.nbt.annotation;

import org.cloudburstmc.server.nbt.NbtValueCodec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Uses a codec to read and write a property. The codec takes precedence over {@link NbtTag}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NbtCodec {

    /**
     * A codec class with a no-argument constructor. Instances are shared and must be thread-safe.
     */
    Class<? extends NbtValueCodec<?>> value();
}
