package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.persistence.StoredJsonList;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage shared by player access lists. Entries are keyed by UUID, or by name while reserved.
 */
public abstract class StoredUserList<E extends StoredUserEntry> extends StoredJsonList<StoredUserList.Key, E> {

    private final Identifier<E> identifier;

    protected StoredUserList(Path path, Class<E[]> entryType, Identifier<E> identifier) {
        super(path, entryType, StoredUserList::key);
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    public boolean contains(UUID uuid) {
        return this.contains(Key.of(uuid));
    }

    public Optional<E> get(UUID uuid) {
        return this.get(Key.of(uuid));
    }

    public void remove(UUID uuid) {
        this.remove(Key.of(uuid));
    }

    /**
     * Returns the entry reserving the given name for a player who has not signed in yet.
     */
    public Optional<E> getReserved(String name) {
        return this.get(Key.of(name));
    }

    public void removeReserved(String name) {
        this.remove(Key.of(name));
    }

    /**
     * Gives the entry reserving the player's name to the signed-in player.
     *
     * @return whether a reserved entry was given to the player
     */
    public synchronized boolean claim(UUID uuid, String name) {
        Key reservedKey = Key.of(name);
        Optional<E> reserved = this.get(reservedKey);
        if (reserved.isEmpty()) {
            return false;
        }

        E claimed = this.identifier.identify(reserved.get(), uuid, name);
        this.update(entries -> {
            entries.remove(reservedKey);
            entries.putIfAbsent(Key.of(uuid), claimed);
        });

        return true;
    }

    private static Key key(StoredUserEntry entry) {
        UUID uuid = entry.uuid();
        if (uuid != null) {
            return Key.of(uuid);
        }

        return Key.of(Objects.requireNonNull(entry.name(), "Entry needs a UUID or a name"));
    }

    @FunctionalInterface
    protected interface Identifier<E> {

        E identify(E reserved, UUID uuid, String name);
    }

    /**
     * Identifies an entry by UUID, or by lowercase name while reserved.
     */
    public record Key(@Nullable UUID uuid, @Nullable String name) {

        static Key of(UUID uuid) {
            return new Key(Objects.requireNonNull(uuid, "uuid"), null);
        }

        static Key of(String name) {
            return new Key(null, Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT));
        }
    }
}
