package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.serializer.util.SlabUtils;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_TOP_SLOT_BIT;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class StoneSlabBlockSerializer implements BlockSerializer {
    public static final BlockSerializer INSTANCE = new StoneSlabBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        StoneSlabType type = state.ensureTrait(BlockTraits.STONE_SLAB_TYPE);
        SlabUtils.SlabInfo info = SlabUtils.getSlabInfo(type);
        builder.putString("name", info.getType().toString());
        builder.putCompound(TAG_STATES, NbtMap.builder()
                .putString(info.getStateName(), info.getState())
                .putBoolean(TAG_TOP_SLOT_BIT, state.ensureTrait(BlockTraits.IS_TOP_SLOT))
                .build());
    }
}
