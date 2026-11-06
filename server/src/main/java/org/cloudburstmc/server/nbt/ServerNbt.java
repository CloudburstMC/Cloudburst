package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.nbt.NbtMapper;

/**
 * The server's NBT mapper, able to write the runtime types data objects hold:
 * item stacks, item containers, skins and block states.
 */
public final class ServerNbt {

    public static final NbtMapper MAPPER = NbtMapper.create(new ServerTypesNbtModule(), new BlockStateNbtModule());

    private ServerNbt() {
    }
}
