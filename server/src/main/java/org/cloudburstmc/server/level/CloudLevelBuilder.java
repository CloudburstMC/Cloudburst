package org.cloudburstmc.server.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.LevelBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.provider.migration.LevelDBImportProfile;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Collects level configuration for an immutable request or an asynchronous load.
 * Defaults to the Overworld, a random seed and the server's fallback generator with no options.
 * Storage and import encodings are detected unless explicitly selected.
 * Building or loading captures the current settings without consuming this builder.
 */
@NotThreadSafe
public class CloudLevelBuilder implements LevelBuilder {

    private final CloudServer server;
    private final String id;

    private int dimension = CloudLevel.DIMENSION_OVERWORLD;
    private long seed;
    private Identifier generator;
    private String generatorOptions = "";
    private @Nullable Identifier storage;
    private @Nullable LevelDBImportProfile importProfile;

    /**
     * Creates a builder with the server's default creation settings.
     *
     * @param server server that will load the level
     * @param id level identity and single storage directory name
     * @throws IllegalArgumentException if the ID is blank or is not a single directory name
     */
    public CloudLevelBuilder(CloudServer server, String id) {
        this.server = Objects.requireNonNull(server, "server");
        CloudLevelLoadRequest.validateId(id);
        this.id = id;
        this.seed = ThreadLocalRandom.current().nextLong();
        this.generator = Objects.requireNonNull(server.getGeneratorRegistry().getFallback(), "fallback generator");
    }

    /**
     * Sets the fallback seed used when saved metadata supplies none.
     *
     * @param seed level seed
     * @return this builder
     */
    @Override
    public CloudLevelBuilder seed(long seed) {
        this.seed = seed;
        return this;
    }

    /**
     * Sets the level's dimension, overriding the dimension in saved metadata.
     *
     * @param dimension 0 for Overworld, 1 for Nether or 2 for End
     * @return this builder
     * @throws IllegalArgumentException if the dimension is unsupported
     */
    @Override
    public CloudLevelBuilder dimension(int dimension) {
        CloudLevelLoadRequest.validateDimension(dimension);
        this.dimension = dimension;
        return this;
    }

    /**
     * Selects the generator for new levels and new terrain in imported levels.
     * Existing native levels retain their saved generator.
     *
     * @param generator registered generator ID
     * @return this builder
     * @throws IllegalArgumentException if the generator is not registered
     */
    @Override
    public CloudLevelBuilder generator(Identifier generator) {
        Objects.requireNonNull(generator, "generator");
        if (!this.server.getGeneratorRegistry().isRegistered(generator)) {
            throw new IllegalArgumentException("Unknown generator: " + generator);
        }

        this.generator = generator;
        return this;
    }

    /**
     * Sets options for the selected creation or import generator.
     *
     * @param generatorOptions generator options, or {@code null} for an empty string
     * @return this builder
     */
    @Override
    public CloudLevelBuilder generatorOptions(@Nullable String generatorOptions) {
        this.generatorOptions = Objects.requireNonNullElse(generatorOptions, "");
        return this;
    }

    /**
     * Selects the storage provider used after any required import instead of detecting it.
     *
     * @param storage registered storage ID
     * @return this builder
     * @throws IllegalArgumentException if the storage provider is not registered
     */
    public CloudLevelBuilder storage(Identifier storage) {
        Objects.requireNonNull(storage, "storage");
        if (!this.server.getStorageRegistry().isRegistered(storage)) {
            throw new IllegalArgumentException("Unknown storage provider: " + storage);
        }

        this.storage = storage;
        return this;
    }

    /**
     * Selects a LevelDB import encoding when source metadata is ambiguous.
     *
     * @param profile source encoding, or {@code null} for detection
     * @return this builder
     * @throws IllegalArgumentException if native Cloudburst storage is selected
     */
    public CloudLevelBuilder importProfile(@Nullable LevelDBImportProfile profile) {
        if (profile == LevelDBImportProfile.CLOUDBURST) {
            throw new IllegalArgumentException("Cloudburst storage is not an import profile");
        }

        this.importProfile = profile;
        return this;
    }

    /**
     * Loads, imports or creates the level using a snapshot of the current settings.
     * An already loaded level is returned unchanged. Concurrent loads for the same ID
     * share the operation already in progress.
     *
     * @return future completed with the initialized and registered level
     */
    @Override
    public CompletableFuture<CloudLevel> load() {
        return this.server.getLevelManager().load(this.build());
    }

    /**
     * Captures this configuration without starting a level load.
     *
     * @return immutable load request
     */
    public CloudLevelLoadRequest build() {
        return new CloudLevelLoadRequest(
                this.id,
                this.seed,
                this.dimension,
                this.generator,
                this.generatorOptions,
                this.storage,
                this.importProfile
        );
    }
}
