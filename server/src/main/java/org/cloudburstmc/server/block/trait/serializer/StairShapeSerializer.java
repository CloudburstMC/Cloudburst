package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.data.StairShape;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * Encodes the straight stair shape using its Bedrock state value.
 */
@ParametersAreNonnullByDefault
public class StairShapeSerializer implements TraitSerializer<StairShape> {

    @Override
    public Comparable<?> serialize(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<StairShape> trait, StairShape value) {
        return value == StairShape.STRAIGHT ? "none" : value;
    }
}
