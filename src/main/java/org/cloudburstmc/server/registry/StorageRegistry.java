package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.world.provider.WorldProviderFactory;
import org.cloudburstmc.server.world.provider.anvil.AnvilProviderFactory;
import org.cloudburstmc.server.world.provider.leveldb.LevelDBProviderFactory;
import org.cloudburstmc.server.world.storage.StorageIds;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;

@Log4j2
public class StorageRegistry implements Registry {
    private static final StorageRegistry INSTANCE = new StorageRegistry();
    private final Map<Identifier, WorldProviderFactory> providers = new IdentityHashMap<>();
    private final List<WeightedProvider> detectProviders = new ArrayList<>();
    private volatile boolean closed;

    private StorageRegistry() {
        this.registerVanillaStorage();
    }

    public static StorageRegistry get() {
        return INSTANCE;
    }

    public synchronized void register(Identifier identifier, WorldProviderFactory worldProviderFactory, int weight)
            throws RegistryException {
        Objects.requireNonNull(identifier, "type");
        Objects.requireNonNull(worldProviderFactory, "worldProviderFactory");

        Preconditions.checkArgument(!this.providers.containsKey(identifier));
        this.providers.put(identifier, worldProviderFactory);
        this.detectProviders.add(new WeightedProvider(identifier, worldProviderFactory, weight));
        this.detectProviders.sort(Comparator.naturalOrder());
    }

    public WorldProviderFactory getLevelProviderFactory(Identifier identifier) {
        Objects.requireNonNull(identifier, "identifier");

        return this.providers.get(identifier);
    }

    public boolean isRegistered(Identifier identifier) {
        return this.providers.containsKey(identifier);
    }

    @Nullable
    public Identifier detectStorage(String levelId, Path levelsPath) {
        for (WeightedProvider weightedProvider : detectProviders) {
            if (weightedProvider.factory.isCompatible(levelId, levelsPath)) {
                return weightedProvider.identifier;
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

    @ToString
    @RequiredArgsConstructor
    private static class WeightedProvider implements Comparable<WeightedProvider> {
        private final Identifier identifier;
        private final WorldProviderFactory factory;
        private final int weight;

        @Override
        public int compareTo(WeightedProvider o) {
            return Integer.compare(this.weight, o.weight);
        }
    }
}
