package org.cloudburstmc.server.block.serializer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_LIQUID_DEPTH;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FluidBlockSerializer implements BlockSerializer {
    public static final BlockSerializer INSTANCE = new FluidBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        NbtMapBuilder statesBuilder = NbtMap.builder();
        int liquidDepth = (Integer) traits.getOrDefault(BlockTraits.LIQUID_DEPTH, 0);

        statesBuilder.putInt(TAG_LIQUID_DEPTH, liquidDepth);
        builder.putCompound(TAG_STATES, statesBuilder.build());
    }
}
