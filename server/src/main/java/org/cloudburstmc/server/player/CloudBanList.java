package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.ban.BanList;
import org.cloudburstmc.server.persistence.StoredJsonList;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class CloudBanList<T, E> implements BanList<T> {

    private final StoredJsonList<String, E> storage;

    protected CloudBanList(Path path, Class<E[]> type, Function<E, String> keys) {
        storage = new StoredJsonList<>(path, type, keys);
    }

    @Override
    public @Nullable BanEntry<T> getBanEntry(T target) {
        return storage.get(key(target)).map(this::decode).filter(entry -> !entry.isExpired()).orElse(null);
    }

    @Override
    public Set<BanEntry<T>> getBanEntries() {
        Set<BanEntry<T>> result = new LinkedHashSet<>();
        for (E saved : storage.entries()) {
            BanEntry<T> entry = decode(saved);
            if (!entry.isExpired()) {
                result.add(entry);
            }
        }

        return Collections.unmodifiableSet(result);
    }

    @Override
    public BanEntry<T> addBan(T target, @Nullable String reason, @Nullable Instant expiration, @Nullable String source) {
        BanEntry<T> entry = new BanEntry<>(target, Instant.now(), source == null ? "(Unknown)" : source,
                expiration, reason == null ? "Banned by an operator." : reason);
        storage.add(encode(entry));
        return entry;
    }

    @Override
    public void pardon(T target) {
        storage.remove(key(target));
    }

    public void reload() {
        storage.reload();
    }

    protected List<E> storedEntries() {
        return storage.entries();
    }

    protected abstract String key(T target);

    protected abstract E encode(BanEntry<T> entry);

    protected abstract BanEntry<T> decode(E entry);
}
