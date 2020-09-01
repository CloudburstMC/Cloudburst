package org.cloudburstmc.server.block.trait.serializer;

import lombok.val;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PoweredSerializer implements TraitSerializer<Boolean> {

    @Override
    public String getName(BlockState state, BlockTrait<?> blockTrait) {
        val type = state.getType();

        if (type == BlockIds.RAIL || type == BlockIds.ACTIVATOR_RAIL || type == BlockIds.GOLDEN_RAIL || type == BlockIds.DETECTOR_RAIL) {
            return "rail_data_bit";
        }

        return null;
    }
}
