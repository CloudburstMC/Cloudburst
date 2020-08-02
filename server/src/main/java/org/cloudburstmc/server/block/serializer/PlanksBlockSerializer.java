package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.serializer.util.TreeSpeciesUtils;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_WOOD_TYPE;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PlanksBlockSerializer implements BlockSerializer {
    public static final BlockSerializer INSTANCE = new PlanksBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        TreeSpecies species = state.ensureTrait(BlockTraits.TREE_SPECIES);
        TreeSpeciesUtils.Info info = TreeSpeciesUtils.getInfo(species);
        builder.putCompound(TAG_STATES, NbtMap.builder()
                .putString(TAG_WOOD_TYPE, info.getState())
                .build());
    }
}
