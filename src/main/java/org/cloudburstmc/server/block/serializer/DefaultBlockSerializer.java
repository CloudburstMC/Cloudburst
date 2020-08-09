package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;

public class DefaultBlockSerializer implements BlockSerializer {

    public static final DefaultBlockSerializer INSTANCE = new DefaultBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        NbtMapBuilder statesBuilder = NbtMap.builder();
        state.getTraits().forEach((trait, value) ->
                BlockTraitSerializers.serialize(statesBuilder, state, trait, value)
        );

        builder.putCompound(TAG_STATES, statesBuilder.build());
    }
}
