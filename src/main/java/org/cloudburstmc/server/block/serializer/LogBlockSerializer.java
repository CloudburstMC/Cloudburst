package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.serializer.util.TreeSpeciesUtils;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.data.TreeSpecies;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LogBlockSerializer implements BlockSerializer {
    public static final BlockSerializer INSTANCE = new LogBlockSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        TreeSpecies species = state.ensureTrait(BlockTraits.TREE_SPECIES);
        Direction.Axis axis = state.ensureTrait(BlockTraits.AXIS);

        TreeSpeciesUtils.Info info = TreeSpeciesUtils.getInfo(species);

        builder.putString(TAG_NAME, info.getLogType().toString());
        builder.putCompound(TAG_STATES, NbtMap.builder()
                .putString(info.getLogStateName(), info.getState())
                .putString("pillar_axis", axis.name().toLowerCase())
                .build());
    }
}
