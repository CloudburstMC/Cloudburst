package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;

public class DefaultBlockSerializer implements BlockSerializer {

    public static final DefaultBlockSerializer INSTANCE = new DefaultBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        state.getTraits().forEach((trait, value) ->
                BlockTraitSerializers.serialize(builder, state, trait, value)
        );
    }
}
