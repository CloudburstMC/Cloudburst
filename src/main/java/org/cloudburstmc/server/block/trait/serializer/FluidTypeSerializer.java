package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.FluidType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class FluidTypeSerializer implements TraitSerializer<FluidType> {

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        if (type == BlockTypes.CAULDRON) {
            return "cauldron_liquid";
        }

        return null;
    }
}
