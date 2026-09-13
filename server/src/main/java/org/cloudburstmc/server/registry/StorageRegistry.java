package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.registry.Registry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.provider.LevelProviderFactory;
import org.cloudburstmc.server.level.provider.anvil.AnvilProviderFactory;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBProviderFactory;
import org.cloudburstmc.server.level.storage.StorageIds;

import java.nio.file.Path;
import java.util.*;

public class StorageRegistry implements Registry {
    private static final StorageRegistry INSTANCE = new StorageRegistry();
    private final Map<Identifier, LevelProviderFactory> providers = new HashMap<>();
    private final List<WeightedProvider> detectProviders = new ArrayList<>();
    private volatile boolean closed;

    private StorageRegistry() {
        this.registerVanillaStorage();
    }

    public static StorageRegistry get() {
        return INSTANCE;
    }

    public synchronized void register(Identifier identifier, LevelProviderFactory levelProviderFactory, int weight) throws RegistryException {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(levelProviderFactory, "levelProviderFactory");

        Preconditions.checkState(!this.closed, "Registry is closed");
        Preconditions.checkArgument(!this.providers.containsKey(identifier));

        this.providers.put(identifier, levelProviderFactory);
        this.detectProviders.add(new WeightedProvider(identifier, levelProviderFactory, weight));
        this.detectProviders.sort(Comparator.naturalOrder());
    }

    public @Nullable LevelProviderFactory getLevelProviderFactory(Identifier identifier) {
        Objects.requireNonNull(identifier, "identifier");
        return this.providers.get(identifier);
    }

    public boolean isRegistered(Identifier identifier) {
        return this.providers.containsKey(identifier);
    }

    @Nullable
    public Identifier detectStorage(String levelId, Path levelsPath) {
        for (WeightedProvider weightedProvider : detectProviders) {
            if (weightedProvider.factory().isCompatible(levelId, levelsPath)) {
                return weightedProvider.identifier();
            }
        }

        return null;
    }

    @Override
    public void close() {
        Preconditions.checkArgument(!this.closed, "Registry has already been closed");
        this.closed = true;
    }

    private void registerVanillaStorage() throws RegistryException {
        this.register(StorageIds.ANVIL, AnvilProviderFactory.INSTANCE, Integer.MIN_VALUE);
        this.register(StorageIds.LEVELDB, LevelDBProviderFactory.INSTANCE, 100);
    }

    private record WeightedProvider(
            Identifier identifier,
            LevelProviderFactory factory,
            int weight
    ) implements Comparable<WeightedProvider> {

        @Override
        public int compareTo(WeightedProvider other) {
            return Integer.compare(this.weight, other.weight);
        }
    }
}
