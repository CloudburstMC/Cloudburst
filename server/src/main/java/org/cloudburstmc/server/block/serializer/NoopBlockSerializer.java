package org.cloudburstmc.server.block.serializer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopBlockSerializer implements BlockSerializer {

    public static final NoopBlockSerializer INSTANCE = new NoopBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {

    }
}
