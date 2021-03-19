package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.*;

@ParametersAreNonnullByDefault
public class StoneSlabSerializer implements TraitSerializer<StoneSlabType> {

    private static final String[] BEDROCK_TRAITS = {
            TAG_STONE_SLAB_TYPE,
            TAG_STONE_SLAB_TYPE_2,
            TAG_STONE_SLAB_TYPE_3,
            TAG_STONE_SLAB_TYPE_4,
    };

    @Override
    public String getName(BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        StoneSlabType type = (StoneSlabType) traits.getOrDefault(BlockTraits.STONE_SLAB_TYPE, traits.get(BlockTraits.STONE_STAIRS_TYPE));

        int index = blockTrait.getIndex(type);

        if (blockType != BlockTypes.STONE_SLAB) {
            if (index >= 2) { //skip wood type
                index++;
            }
        }

        if (index > 28) {
            return null;
        }

        return BEDROCK_TRAITS[index >> 3];
    }
}
