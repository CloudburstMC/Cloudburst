package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTrait;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopBlockSerializer implements BlockSerializer {

    public static final NoopBlockSerializer INSTANCE = new NoopBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {

    }
}
