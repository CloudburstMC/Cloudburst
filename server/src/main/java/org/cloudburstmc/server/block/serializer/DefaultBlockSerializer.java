package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DefaultBlockSerializer implements BlockSerializer {

    public static final DefaultBlockSerializer INSTANCE = new DefaultBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        NbtMapBuilder statesBuilder = NbtMap.builder();
        traits.forEach((trait, value) ->
                BlockTraitSerializers.serialize(statesBuilder, blockType, traits, trait, value)
        );

        builder.putCompound(TAG_STATES, statesBuilder.build());
    }
}
