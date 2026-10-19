package org.cloudburstmc.server.player;

import org.cloudburstmc.server.persistence.StoredJsonList;

import java.nio.file.Path;
import java.util.UUID;

/**
 * UUID-keyed storage shared by player access lists.
 */
public abstract class StoredUserList<E extends StoredUserEntry> extends StoredJsonList<UUID, E> {

    protected StoredUserList(Path path, Class<E[]> entryType) {
        super(path, entryType, StoredUserEntry::uuid);
    }
}
