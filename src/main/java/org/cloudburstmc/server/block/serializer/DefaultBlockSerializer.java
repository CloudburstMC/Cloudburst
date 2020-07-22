package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTrait;

public class DefaultBlockSerializer implements BlockSerializer {

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        for (BlockTrait<?> trait : state.getTraits().keySet()) {
            serialize(builder, state, trait);
        }
    }
}
