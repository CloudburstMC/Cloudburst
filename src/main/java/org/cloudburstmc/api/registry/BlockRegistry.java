package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.block.BlockType;

public abstract class BlockRegistry {

    private static BlockRegistry INSTANCE;

    public BlockRegistry() {
        INSTANCE = this;
    }

    public static BlockRegistry get() {
        return INSTANCE;
    }

    public abstract BlockBehavior getBehavior(BlockType type);
}
