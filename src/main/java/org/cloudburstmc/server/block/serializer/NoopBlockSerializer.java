package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockState;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopBlockSerializer implements BlockSerializer {
    public static final NoopBlockSerializer INSTANCE = new NoopBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        // no-op
    }
}
