package org.cloudburstmc.server.block.trait.serializer;

import lombok.val;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TreeSpeciesSerializer implements TraitSerializer<TreeSpecies> {

    @Override
    public String getName(BlockState state, BlockTrait<?> blockTrait) {
        val type = state.getType();

        if (type == BlockTypes.LOG) {
            return "old_log_type";
        }

        if (type == BlockTypes.LOG2) {
            return "new_log_type";
        }

        if (type == BlockTypes.PLANKS) {
            return "wood_type";
        }

        if (type == BlockTypes.LEAVES) {
            return "old_leaf_type";
        }

        if (type == BlockTypes.LEAVES2) {
            return "new_leaf_type";
        }

        if (type == BlockTypes.SAPLING) {
            return "sapling_type";
        }

        if (type == BlockTypes.BAMBOO_SAPLING) {
            return "sapling_type";
        }

        return "wood_type";
    }
}
