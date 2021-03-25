package org.cloudburstmc.server.level.provider.leveldb;

import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.utils.NibbleArray;

public class BlockStorageConverter {

    public static BlockStorage fromXZY(byte[] blockIds, byte[] blockData) {
        NibbleArray data = new NibbleArray(blockData);

        BlockStorage storage = new BlockStorage();

        for (int i = 0; i < 4096; i++) {
            storage.setBlock(i, CloudBlockRegistry.get().getBlock(blockIds[i], data.get(i)));
        }

        return storage;
    }
}
