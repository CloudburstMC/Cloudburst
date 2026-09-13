package org.cloudburstmc.server.level.provider;

import org.cloudburstmc.server.level.CloudLevelData;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads and writes a provider's persisted level state.
 */
public interface CloudLevelDataSerializer {

    /**
     * Loads persisted level state.
     *
     * @param initialData complete state copied before persisted fields are applied
     * @param levelPath   the provider's level directory
     * @return the loaded state, or empty when the level does not exist
     * @throws IOException if the state cannot be read
     */
    Optional<CloudLevelData> load(CloudLevelData initialData, Path levelPath) throws IOException;

    /**
     * Saves the current level state.
     *
     * @param data      the level data to persist
     * @param levelPath the provider's level directory
     * @throws IOException if the state cannot be written
     */
    void save(CloudLevelData data, Path levelPath) throws IOException;
}
