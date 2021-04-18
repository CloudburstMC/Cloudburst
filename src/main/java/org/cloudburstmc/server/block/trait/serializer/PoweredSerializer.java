package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class PoweredSerializer implements TraitSerializer<Boolean> {

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        if (type == BlockTypes.RAIL || type == BlockTypes.ACTIVATOR_RAIL || type == BlockTypes.GOLDEN_RAIL || type == BlockTypes.DETECTOR_RAIL) {
            return "rail_data_bit";
        }

        return null;
    }
}
