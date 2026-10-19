package org.cloudburstmc.server.level.storage;

import org.cloudburstmc.api.util.Identifier;

public class StorageIds {

    public static final Identifier LEVELDB = Identifier.parse("minecraft:leveldb");

    private StorageIds() {
        throw new UnsupportedOperationException();
    }
}
