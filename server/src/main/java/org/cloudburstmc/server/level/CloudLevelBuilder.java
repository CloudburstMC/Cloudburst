package org.cloudburstmc.server.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.LevelBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.CloudServer;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds an immutable request to load or create a level.
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

    public CloudLevelBuilder(CloudServer server, String id) {
        this.server = Objects.requireNonNull(server, "server");
        CloudLevelLoadRequest.validateId(id);
        this.id = id;
        this.seed = ThreadLocalRandom.current().nextLong();
        this.generator = Objects.requireNonNull(server.getGeneratorRegistry().getFallback(), "fallback generator");
    }

    @Override
    public CloudLevelBuilder seed(long seed) {
        this.seed = seed;
        return this;
    }

    @Override
    public CloudLevelBuilder dimension(int dimension) {
        CloudLevelLoadRequest.validateDimension(dimension);
        this.dimension = dimension;
        return this;
    }

    @Override
    public CloudLevelBuilder generator(Identifier generator) {
        Objects.requireNonNull(generator, "generator");
        if (!this.server.getGeneratorRegistry().isRegistered(generator)) {
            throw new IllegalArgumentException("Unknown generator: " + generator);
        }

        this.generator = generator;
        return this;
    }

    @Override
    public CloudLevelBuilder generatorOptions(@Nullable String generatorOptions) {
        this.generatorOptions = Objects.requireNonNullElse(generatorOptions, "");
        return this;
    }

    /**
     * Selects a server storage provider instead of detecting existing data.
     *
     * @param storage registered storage ID
     * @return this builder
     */
    public CloudLevelBuilder storage(Identifier storage) {
        Objects.requireNonNull(storage, "storage");
        if (!this.server.getStorageRegistry().isRegistered(storage)) {
            throw new IllegalArgumentException("Unknown storage provider: " + storage);
        }

        this.storage = storage;
        return this;
    }

    @Override
    public CompletableFuture<CloudLevel> load() {
        CloudLevelLoadRequest request = new CloudLevelLoadRequest(
                this.id,
                this.seed,
                this.dimension,
                this.generator,
                this.generatorOptions,
                this.storage
        );

        return this.server.getLevelManager().load(request);
    }
}
