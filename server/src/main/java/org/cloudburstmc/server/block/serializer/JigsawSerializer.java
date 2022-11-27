package org.cloudburstmc.server.block.serializer;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

public class JigsawSerializer implements BlockSerializer {

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        NbtMapBuilder statesBuilder = NbtMap.builder();

        statesBuilder.putInt("facing_direction", ((Direction) traits.get(BlockTraits.FACING_DIRECTION)).ordinal()); //TODO: check

        Direction direction = (Direction) traits.get(BlockTraits.DIRECTION);
        statesBuilder.putInt("rotation", BlockTraits.DIRECTION.getIndex(direction)); //TODO: check

        builder.putCompound(TAG_STATES, statesBuilder.build());
    }
}
