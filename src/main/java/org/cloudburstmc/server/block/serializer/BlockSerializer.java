package org.cloudburstmc.server.block.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.List;
import java.util.Map;

public interface BlockSerializer {

    String TAG_NAME = "name";
    String TAG_STATES = "states";

    void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits);

    default void serialize(List<NbtMapBuilder> tags, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        NbtMapBuilder builder = NbtMap.builder();
        serialize(builder, blockType, traits);
        tags.add(builder);
    }
}
