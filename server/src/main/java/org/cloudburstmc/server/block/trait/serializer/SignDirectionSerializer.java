package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class SignDirectionSerializer implements TraitSerializer<CardinalDirection> {

    @Override
    public Comparable<?> serialize(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<CardinalDirection> trait, CardinalDirection direction) {
        return direction.ordinal();
    }

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<CardinalDirection> blockTrait) {
        return BedrockStateTags.TAG_GROUND_SIGN_DIRECTION;
    }
}
