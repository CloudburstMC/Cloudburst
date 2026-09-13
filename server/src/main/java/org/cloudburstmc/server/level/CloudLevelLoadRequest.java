package org.cloudburstmc.server.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Identifier;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable configuration for loading or creating a level.
 *
 * @param id               level identity and storage directory name
 * @param seed             seed used when persisted level data does not exist
 * @param dimension        configured dimension
 * @param generator        generator used when persisted level data does not exist
 * @param generatorOptions generator options used when persisted level data does not exist
 * @param storage          explicitly selected storage format, or {@code null} for detection
 */
public record CloudLevelLoadRequest(
        String id,
        long seed,
        int dimension,
        Identifier generator,
        String generatorOptions,
        @Nullable Identifier storage
) {
    public CloudLevelLoadRequest {
        validateId(id);
        validateDimension(dimension);
        Objects.requireNonNull(generator, "generator");
        Objects.requireNonNull(generatorOptions, "generatorOptions");
    }

    /**
     * Validates a level ID used as a directory name.
     *
     * @param id level ID
     */
    public static void validateId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }

        Path idPath = Path.of(id);
        if (idPath.isAbsolute() || idPath.getNameCount() != 1 || id.equals(".") || id.equals("..")) {
            throw new IllegalArgumentException("id must be a single directory name: " + id);
        }
    }

    /**
     * Validates a dimension ID.
     *
     * @param dimension dimension ID
     */
    public static void validateDimension(int dimension) {
        if (dimension < CloudLevel.DIMENSION_OVERWORLD || dimension > CloudLevel.DIMENSION_THE_END) {
            throw new IllegalArgumentException("Unknown dimension: " + dimension);
        }
    }

    /**
     * Creates new level state from this request and the server defaults.
     *
     * @param defaults server-wide defaults
     * @return initialized level state
     */
    public CloudLevelData createInitialData(CloudLevelDefaults defaults) {
        CloudLevelData data = CloudLevelData.fromDefaults(defaults);
        data.setName(this.id);
        data.setSeed(this.seed);
        data.setDimension(this.dimension);
        data.setGenerator(this.generator);
        data.setGeneratorOptions(this.generatorOptions);
        return data;
    }
}
