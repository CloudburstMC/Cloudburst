package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTrait;

import java.util.Map;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_LIQUID_DEPTH;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FluidBlockSerializer implements BlockSerializer {
    public static final BlockSerializer INSTANCE = new FluidBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        NbtMapBuilder statesBuilder = NbtMap.builder();
        int fluidLevel = (int) traits.get(BlockTraits.FLUID_LEVEL);
        boolean flowing = (boolean) traits.get(BlockTraits.IS_FLOWING);
        if (flowing) {
            fluidLevel |= 8;
        }

        statesBuilder.putInt(TAG_LIQUID_DEPTH, fluidLevel);
        builder.putCompound(TAG_STATES, statesBuilder.build());
    }
}
