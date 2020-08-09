package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.SeaGrassType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SeagrassSerializer implements TraitSerializer<SeaGrassType> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockState state, SeaGrassType seaGrassType) {
        if (seaGrassType == SeaGrassType.DOUBLE_BOTTOM) {
            return "double_bot";
        }

        return null;
    }
}
