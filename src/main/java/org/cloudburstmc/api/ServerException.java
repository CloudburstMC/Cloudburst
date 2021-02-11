package org.cloudburstmc.api;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ServerException extends RuntimeException {
    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
