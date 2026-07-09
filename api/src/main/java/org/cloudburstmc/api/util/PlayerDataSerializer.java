package org.cloudburstmc.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public interface PlayerDataSerializer {

    /**
     * Opens player data if it exists.
     *
     * @param key player data key
     * @return stream for existing player data
     */
    Optional<InputStream> read(PlayerDataKey key) throws IOException;

    /**
     * Opens a stream for writing player data.
     *
     * @param key player data key
     * @return stream for writing player data
     */
    OutputStream write(PlayerDataKey key) throws IOException;
}
