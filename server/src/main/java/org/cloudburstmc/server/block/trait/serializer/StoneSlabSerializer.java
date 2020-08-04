package org.cloudburstmc.server.block.trait.serializer;

import lombok.val;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import javax.annotation.ParametersAreNonnullByDefault;

import static org.cloudburstmc.server.block.BlockTypes.*;

@ParametersAreNonnullByDefault
public class StoneSlabSerializer implements TraitSerializer<StoneSlabType> {

    @Override
    public String getName(BlockState state, BlockTrait<?> blockTrait) {
        val type = state.getType();

        if (type == STONE_SLAB || type == DOUBLE_STONE_SLAB) {
            return "stone_slab_type";
        }

        if (type == STONE_SLAB2 || type == DOUBLE_STONE_SLAB2) {
            return "stone_slab_type_2";
        }

        if (type == STONE_SLAB3 || type == DOUBLE_STONE_SLAB3) {
            return "stone_slab_type_3";
        }

        if (type == STONE_SLAB4 || type == DOUBLE_STONE_SLAB4) {
            return "stone_slab_type_4";
        }
        return null;
    }
}
