package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitNameSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class WoodTypeSerializer implements TraitNameSerializer {

    @Override
    public String apply(BlockState state, BlockTrait<?> blockTrait) {
        if (state.getType() == BlockTypes.LOG) {
            return "old_log_type";
        }

        if (state.getType() == BlockTypes.LOG2) {
            return "new_log_type";
        }

        if (state.getType() == BlockTypes.PLANKS) {
            return "wood_type";
        }

        return null;
    }
}
