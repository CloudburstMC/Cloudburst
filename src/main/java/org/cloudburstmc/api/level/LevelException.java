package org.cloudburstmc.api.level;

import org.cloudburstmc.api.ServerException;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class LevelException extends ServerException {
    public LevelException(String message) {
        super(message);
    }

    public LevelException(String message, Throwable cause) {
        super(message, cause);
    }
}
