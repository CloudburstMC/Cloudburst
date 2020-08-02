package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_LIQUID_DEPTH;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FluidBlockSerializer implements BlockSerializer {
    public static final BlockSerializer INSTANCE = new FluidBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        int fluidLevel = state.ensureTrait(BlockTraits.FLUID_LEVEL);
        boolean flowing = state.ensureTrait(BlockTraits.IS_FLOWING);
        if (flowing) {
            fluidLevel |= 8;
        }

        builder.putCompound(TAG_STATES, NbtMap.builder()
                .putInt(TAG_LIQUID_DEPTH, fluidLevel)
                .build());
    }
}
