package org.cloudburstmc.api.util;

import org.cloudburstmc.nbt.NbtMap;

import java.io.IOException;
import java.util.Optional;

/**
 * Stores complete player records. Implementations may be called from background save threads.
 * Storage callbacks must not recursively invoke player-data reads or saves.
 */
public interface PlayerDataSerializer {

    /**
     * Reads an existing player record, recovering from a backup when supported.
     * An empty result means no record exists, not that an existing record is unreadable.
     *
     * @param key player data key
     * @return the record, or empty when no stored data exists
     * @throws IOException if existing data cannot be read or recovered
     */
    Optional<NbtMap> read(PlayerDataKey key) throws IOException;

    /**
     * Saves a complete player record. A failed write must not replace the previous record
     * with incomplete data.
     *
     * @param key player data key
     * @param data the complete player record
     * @throws IOException if the record cannot be saved
     */
    void write(PlayerDataKey key, NbtMap data) throws IOException;
}
