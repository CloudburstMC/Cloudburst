package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.util.data.SeaGrassType;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class SeagrassSerializer implements TraitSerializer<SeaGrassType> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, SeaGrassType seaGrassType) {
        if (seaGrassType == SeaGrassType.DOUBLE_BOTTOM) {
            return "double_bot";
        }

        return null;
    }
}
