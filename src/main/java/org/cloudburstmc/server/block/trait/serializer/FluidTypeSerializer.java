package org.cloudburstmc.server.block.trait.serializer;

import lombok.val;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.FluidType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FluidTypeSerializer implements TraitSerializer<FluidType> {

    @Override
    public String getName(BlockState state, BlockTrait<?> blockTrait) {
        val type = state.getType();

        if (type == BlockIds.LAVA_CAULDRON || type == BlockIds.CAULDRON) {
            return "cauldron_liquid";
        }

        return null;
    }
}
