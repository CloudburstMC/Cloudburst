package org.cloudburstmc.api.nbt;

import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Thrown by {@link NbtValueCodec#fromNbt} when the stored NBT value has the wrong kind or
 * shape for the codec, for example a string where it expects a number.
 * <p>
 * The mapper treats it like any other tag that does not fit its property: the property
 * keeps its current value and the mismatch is logged at warn level. Any other exception
 * from a codec fails the read.
 */
public class NbtKindMismatchException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * @param message what was found and what was expected
     */
    public NbtKindMismatchException(String message) {
        super(message);
    }

    /**
     * @param message what was found and what was expected
     * @param cause   the failure that revealed the mismatch, if any
     */
    public NbtKindMismatchException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
