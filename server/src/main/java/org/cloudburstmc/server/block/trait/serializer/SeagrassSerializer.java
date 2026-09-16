package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.data.SeaGrassType;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class SeagrassSerializer implements TraitSerializer<SeaGrassType> {

    @Override
    public Comparable<?> serialize(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<SeaGrassType> trait, SeaGrassType seaGrassType) {
        if (seaGrassType == SeaGrassType.DOUBLE_BOTTOM) {
            return "double_bot";
        }

        return seaGrassType;
    }
}
