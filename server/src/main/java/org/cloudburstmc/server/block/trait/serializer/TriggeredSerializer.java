package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class TriggeredSerializer implements TraitSerializer<Boolean> {

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        if (type == BlockTypes.DETECTOR_RAIL) {
            return "rail_data_bit";
        }

        return null;
    }
}
