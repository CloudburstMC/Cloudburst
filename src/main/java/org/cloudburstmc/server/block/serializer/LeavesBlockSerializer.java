package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.serializer.util.TreeSpeciesUtils;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import static org.cloudburstmc.server.block.BlockTraits.*;
import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_PERSISTENT_BIT;
import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_UPDATE_BIT;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LeavesBlockSerializer implements BlockSerializer {
    public static final BlockSerializer INSTANCE = new LeavesBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        TreeSpecies species = state.ensureTrait(TREE_SPECIES);
        TreeSpeciesUtils.Info info = TreeSpeciesUtils.getInfo(species);

        builder.putString(TAG_NAME, info.getLeavesType().toString());
        builder.putCompound(TAG_STATES, NbtMap.builder()
                .putString(info.getLeafStateName(), info.getState())
                .putBoolean(TAG_PERSISTENT_BIT, state.ensureTrait(IS_PERSISTENT))
                .putBoolean(TAG_UPDATE_BIT, state.ensureTrait(HAS_UPDATE))
                .build());
    }
}
