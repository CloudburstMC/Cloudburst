package org.cloudburstmc.server.level.storage;

import org.cloudburstmc.api.util.Identifier;

public class StorageIds {

    public static final Identifier LEVELDB = Identifier.fromString("minecraft:leveldb");

    public static final Identifier ANVIL = Identifier.fromString("minecraft:anvil");

    private StorageIds() {
        throw new UnsupportedOperationException();
    }
}
