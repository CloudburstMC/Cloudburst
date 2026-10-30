package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * The server's NBT mapper, able to write the runtime types data objects hold:
 * item stacks, slotted items, item containers and block states.
 */
public final class ServerNbt {

    public static final NbtMapper MAPPER = NbtMapper.create(new ServerTypesNbtModule(), new BlockStateNbtModule());

    static {
        ItemContainers.setFactory(CloudContainer::new);
    }

    private ServerNbt() {
    }

    /**
     * Forces this class to initialize, registering the server's item container factory.
     */
    public static void ensureInitialized() {
    }
}
