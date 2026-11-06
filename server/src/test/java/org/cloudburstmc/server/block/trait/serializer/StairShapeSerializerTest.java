package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.data.StairShape;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StairShapeSerializerTest {

    @Test
    void keepsShapeNamesAtTheSerializationBoundary() {
        BlockTraitSerializers.init();
        for (StairShape shape : StairShape.values()) {
            BlockState state = BlockTypes.OAK_STAIRS.getDefaultState().withTrait(BlockTraits.STAIR_SHAPE, shape);
            NbtMapBuilder builder = NbtMap.builder();
            BlockTraitSerializers.serialize(builder, state.getType(), state.getTraits(), BlockTraits.STAIR_SHAPE);

            String expected = shape == StairShape.STRAIGHT ? "none" : shape.name().toLowerCase(Locale.ROOT);
            assertEquals(expected, builder.build().getString("minecraft:corner"));
        }
    }
}
