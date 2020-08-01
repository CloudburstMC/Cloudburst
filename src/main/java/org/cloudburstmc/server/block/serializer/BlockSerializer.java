package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;

public interface BlockSerializer {

    String TAG_STATES = "states";

    void serialize(NbtMapBuilder builder, BlockState state);
}
