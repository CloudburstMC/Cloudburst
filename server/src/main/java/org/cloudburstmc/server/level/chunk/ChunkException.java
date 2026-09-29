package org.cloudburstmc.server.level.chunk;

/**
 * Indicates that a chunk could not be loaded, saved, or encoded.
 */
public final class ChunkException extends RuntimeException {
    public ChunkException(String message) {
        super(message);
    }

    public ChunkException(String message, Throwable cause) {
        super(message, cause);
    }
}
