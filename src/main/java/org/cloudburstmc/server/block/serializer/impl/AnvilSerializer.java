package org.cloudburstmc.server.block.serializer.impl;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.serializer.DirectionHelper;
import org.cloudburstmc.server.block.serializer.DirectionHelper.SeqType;

public class AnvilSerializer implements BlockSerializer {

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        serialize(builder, state, BlockTraits.DAMAGE);
        DirectionHelper.serializeSimple(builder, state, SeqType.TYPE_2);
    }
}
