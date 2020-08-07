package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.math.Direction;

public class JigsawSerializer implements BlockSerializer {

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        NbtMapBuilder statesBuilder = NbtMap.builder();

        statesBuilder.putInt("facing_direction", state.ensureTrait(BlockTraits.FACING_DIRECTION).ordinal()); //TODO: check

        Direction direction = state.ensureTrait(BlockTraits.DIRECTION);
        statesBuilder.putInt("rotation", BlockTraits.DIRECTION.getIndex(direction)); //TODO: check

        builder.putCompound(TAG_STATES, statesBuilder.build());
    }
}
