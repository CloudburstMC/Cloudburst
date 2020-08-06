package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;

public class JigsawSerializer implements BlockSerializer {

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        builder.putInt("facing_direction", state.ensureTrait(BlockTraits.FACING_DIRECTION).ordinal()); //TODO: check
        builder.putInt("rotation", state.ensureTrait(BlockTraits.DIRECTION).ordinal() - 2); //TODO: check
    }
}
