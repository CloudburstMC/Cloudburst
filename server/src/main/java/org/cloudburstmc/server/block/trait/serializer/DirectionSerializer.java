package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class DirectionSerializer implements TraitSerializer<Direction> {

    @Override
    public Comparable<?> serialize(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<Direction> trait, Direction direction) {
        if (trait == BlockTraits.DIRECTION) {
            return horizontalEncoding(type, traits).encode(direction);
        }

        if (trait == BlockTraits.FACING_DIRECTION) {
            if (type == BlockTypes.OBSERVER) {
                return direction.getName();
            }

            DirectionEncoding encoding = type == BlockTypes.END_ROD ? DirectionEncoding.FACING_DOWN_UP_SOUTH_NORTH_EAST_WEST : DirectionEncoding.FACING_DOWN_UP_NORTH_SOUTH_WEST_EAST;
            return encoding.encode(direction);
        }

        throw new IllegalArgumentException("Unsupported direction trait " + trait + " for " + type);
    }

    private static DirectionEncoding horizontalEncoding(
            BlockType type, Map<BlockTrait<?>, Comparable<?>> traits) {
        if (traits.containsKey(BlockTraits.STAIR_SHAPE)
                || traits.containsKey(BlockTraits.IS_OPEN) && traits.containsKey(BlockTraits.IS_UPSIDE_DOWN)) {
            return DirectionEncoding.HORIZONTAL_EAST_WEST_SOUTH_NORTH;
        }

        if (type == BlockTypes.COCOA) {
            return DirectionEncoding.HORIZONTAL_NORTH_EAST_SOUTH_WEST;
        }

        return DirectionEncoding.HORIZONTAL_SOUTH_WEST_NORTH_EAST;
    }

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<Direction> blockTrait) {
        if (blockTrait == BlockTraits.DIRECTION && traits.containsKey(BlockTraits.STAIR_SHAPE)) {
            return BedrockStateTags.TAG_WEIRDO_DIRECTION;
        }

        if (blockTrait == BlockTraits.FACING_DIRECTION && type == BlockTypes.OBSERVER) {
            return BedrockStateTags.TAG_MINECRAFT_FACING_DIRECTION;
        }

        return blockTrait.getVanillaName();
    }
}
