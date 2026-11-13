package org.cloudburstmc.server.nbt;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.nbt.module.BlockStateNbtModule;
import org.cloudburstmc.server.nbt.module.ServerTypesNbtModule;

/**
 * The server's NBT mapper, able to write the runtime types data objects hold:
 * item stacks, item containers, skins and block states.
 */
@UtilityClass
public class ServerNbt {

    public static final NbtMapper MAPPER = NbtMapper.create(new ServerTypesNbtModule(), new BlockStateNbtModule());
}
