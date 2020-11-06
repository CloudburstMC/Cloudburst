package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.*;

@ParametersAreNonnullByDefault
public class TreeSpeciesSerializer implements TraitSerializer<TreeSpecies> {

    private static final String[] BEDROCK_LOG_TRAITS = {
            TAG_OLD_LOG_TYPE,
            TAG_NEW_LOG_TYPE
    };

    private static final String[] BEDROCK_LEAF_TRAITS = {
            TAG_OLD_LEAF_TYPE,
            TAG_NEW_LEAF_TYPE
    };

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        if (type == BlockTypes.PLANKS) {
            return "wood_type";
        }

        if (type == BlockTypes.SAPLING) {
            return "sapling_type";
        }

        if (type == BlockTypes.BAMBOO_SAPLING) {
            return "sapling_type";
        }

        TreeSpecies val = (TreeSpecies) traits.get(BlockTraits.TREE_SPECIES);

        if (val == null) {
            val = (TreeSpecies) traits.get(BlockTraits.TREE_SPECIES_OVERWORLD);
        }

        if (val == null) {
            val = (TreeSpecies) traits.get(BlockTraits.TREE_SPECIES_NETHER);
        }

        int index = val.ordinal() >> 2;

        if (type == BlockTypes.LOG) {
            return BEDROCK_LOG_TRAITS[index];
        }

        if (type == BlockTypes.LEAVES) {
            return BEDROCK_LEAF_TRAITS[index];
        }

        return "wood_type";
    }
}
