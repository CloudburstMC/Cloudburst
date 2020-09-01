package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TriggeredSerializer implements TraitSerializer<Boolean> {

    @Override
    public String getName(BlockState state, BlockTrait<?> blockTrait) {
        if (state.getType() == BlockIds.DETECTOR_RAIL) {
            return "rail_data_bit";
        }

        return null;
    }
}
