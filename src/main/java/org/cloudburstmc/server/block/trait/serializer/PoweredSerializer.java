package org.cloudburstmc.server.block.trait.serializer;

import lombok.val;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PoweredSerializer implements TraitSerializer<Boolean> {

    @Override
    public String getName(BlockState state, BlockTrait<?> blockTrait) {
        val type = state.getType();

        if (type == BlockTypes.RAIL || type == BlockTypes.ACTIVATOR_RAIL || type == BlockTypes.GOLDEN_RAIL || type == BlockTypes.DETECTOR_RAIL) {
            return "rail_data_bit";
        }

        return null;
    }
}
