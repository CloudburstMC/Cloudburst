package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;

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
