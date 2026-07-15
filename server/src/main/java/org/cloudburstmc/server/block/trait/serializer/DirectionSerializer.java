package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.serializer.DirectionHelper;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
public class DirectionSerializer implements TraitSerializer<Direction> {

    private static final Set<BlockType> FACING_DIRECTION_STRING_BLOCKS = Set.of(
            BlockTypes.OBSERVER
    );

    static {
        DirectionHelper.init();
    }

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, Direction direction) {
        if (FACING_DIRECTION_STRING_BLOCKS.contains(type)) {
            return direction.name().toLowerCase();
        }
        return DirectionHelper.serialize(builder, type, traits);
    }

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        if (type.is(BlockTags.STAIRS)) {
            return BedrockStateTags.TAG_WEIRDO_DIRECTION;
        }

        if (FACING_DIRECTION_STRING_BLOCKS.contains(type)) {
            return BedrockStateTags.TAG_MINECRAFT_FACING_DIRECTION;
        }

        return null;
    }
}
