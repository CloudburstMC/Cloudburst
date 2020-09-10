package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTrait;

import java.util.Map;

public interface BlockSerializer {

    String TAG_NAME = "name";
    String TAG_STATES = "states";

    void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits);

    default String getName() {
        return null;
    }
}