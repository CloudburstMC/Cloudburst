package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class StoneSlabBlockSerializer implements BlockSerializer {
    public static final BlockSerializer INSTANCE = new StoneSlabBlockSerializer();

    private static final Identifier[] BLOCK_NAMES = {
            Identifier.fromString("stone_slab"),
            Identifier.fromString("stone_slab2"),
            Identifier.fromString("stone_slab3"),
            Identifier.fromString("stone_slab4"),
    };

    private static final String[] STATE_NAMES = {
            TAG_STONE_SLAB_TYPE,
            TAG_STONE_SLAB_TYPE_2,
            TAG_STONE_SLAB_TYPE_3,
            TAG_STONE_SLAB_TYPE_4
    };

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        StoneSlabType type = state.ensureTrait(BlockTraits.STONE_SLAB_TYPE);
        int index = type.ordinal();

        builder.putString(TAG_NAME, BLOCK_NAMES[index & 7].toString());
        builder.putCompound(TAG_STATES, NbtMap.builder()
                .putString(STATE_NAMES[index & 7], type.name().toLowerCase())
                .putBoolean(TAG_TOP_SLOT_BIT, state.ensureTrait(BlockTraits.IS_TOP_SLOT))
                .build());
    }
}
