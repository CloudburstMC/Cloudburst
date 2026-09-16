package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class DirectionSerializerTest {

    private final DirectionSerializer serializer = new DirectionSerializer();

    @Test
    void serializesAllStairsWithTheHorizontalStairSequence() {
        BlockTypes.values().stream()
                .filter(type -> type.getTraits().contains(BlockTraits.STAIR_SHAPE))
                .forEach(this::assertStairDirection);
    }

    @Test
    void serializesAllTrapdoorsWithTheHorizontalTrapdoorSequence() {
        BlockTypes.values().stream()
                .filter(type -> type.getTraits().contains(BlockTraits.DIRECTION))
                .filter(type -> type.getTraits().contains(BlockTraits.IS_OPEN))
                .filter(type -> type.getTraits().contains(BlockTraits.IS_UPSIDE_DOWN))
                .forEach(this::assertHorizontalEwsnDirection);
    }

    @Test
    void serializesOrdinaryHorizontalDirections() {
        assertAll(
                () -> assertDirection(BlockTypes.BED, BlockTraits.DIRECTION, Direction.SOUTH, 0),
                () -> assertDirection(BlockTypes.BED, BlockTraits.DIRECTION, Direction.WEST, 1),
                () -> assertDirection(BlockTypes.BED, BlockTraits.DIRECTION, Direction.NORTH, 2),
                () -> assertDirection(BlockTypes.BED, BlockTraits.DIRECTION, Direction.EAST, 3)
        );
    }

    @Test
    void serializesCocoaDirections() {
        assertAll(
                () -> assertDirection(BlockTypes.COCOA, BlockTraits.DIRECTION, Direction.NORTH, 0),
                () -> assertDirection(BlockTypes.COCOA, BlockTraits.DIRECTION, Direction.EAST, 1),
                () -> assertDirection(BlockTypes.COCOA, BlockTraits.DIRECTION, Direction.SOUTH, 2),
                () -> assertDirection(BlockTypes.COCOA, BlockTraits.DIRECTION, Direction.WEST, 3)
        );
    }

    @Test
    void serializesFacingDirections() {
        assertAll(
                () -> assertDirection(BlockTypes.DISPENSER, BlockTraits.FACING_DIRECTION, Direction.DOWN, 0),
                () -> assertDirection(BlockTypes.DISPENSER, BlockTraits.FACING_DIRECTION, Direction.UP, 1),
                () -> assertDirection(BlockTypes.DISPENSER, BlockTraits.FACING_DIRECTION, Direction.NORTH, 2),
                () -> assertDirection(BlockTypes.DISPENSER, BlockTraits.FACING_DIRECTION, Direction.SOUTH, 3),
                () -> assertDirection(BlockTypes.DISPENSER, BlockTraits.FACING_DIRECTION, Direction.WEST, 4),
                () -> assertDirection(BlockTypes.DISPENSER, BlockTraits.FACING_DIRECTION, Direction.EAST, 5)
        );
    }

    @Test
    void serializesEndRodFacingDirections() {
        assertAll(
                () -> assertDirection(BlockTypes.END_ROD, BlockTraits.FACING_DIRECTION, Direction.DOWN, 0),
                () -> assertDirection(BlockTypes.END_ROD, BlockTraits.FACING_DIRECTION, Direction.UP, 1),
                () -> assertDirection(BlockTypes.END_ROD, BlockTraits.FACING_DIRECTION, Direction.SOUTH, 2),
                () -> assertDirection(BlockTypes.END_ROD, BlockTraits.FACING_DIRECTION, Direction.NORTH, 3),
                () -> assertDirection(BlockTypes.END_ROD, BlockTraits.FACING_DIRECTION, Direction.EAST, 4),
                () -> assertDirection(BlockTypes.END_ROD, BlockTraits.FACING_DIRECTION, Direction.WEST, 5)
        );
    }

    @Test
    void serializesObserverFacingAsAName() {
        Map<BlockTrait<?>, Comparable<?>> traits = Map.of(BlockTraits.FACING_DIRECTION, Direction.NORTH);

        assertEquals("north", this.serializer.serialize(BlockTypes.OBSERVER, traits, BlockTraits.FACING_DIRECTION, Direction.NORTH));
        assertEquals(BedrockStateTags.TAG_MINECRAFT_FACING_DIRECTION, this.serializer.getName(BlockTypes.OBSERVER, traits, BlockTraits.FACING_DIRECTION));
    }

    @Test
    void rejectsDirectionsOutsideTheSelectedEncoding() {
        Map<BlockTrait<?>, Comparable<?>> traits = Map.of(BlockTraits.DIRECTION, Direction.UP);

        assertThrows(IllegalArgumentException.class, () -> this.serializer.serialize(BlockTypes.BED, traits, BlockTraits.DIRECTION, Direction.UP));
    }

    private void assertStairDirection(BlockType type) {
        assertHorizontalEwsnDirection(type);
        Map<BlockTrait<?>, Comparable<?>> traits = type.getDefaultState().getTraits();
        assertEquals(BedrockStateTags.TAG_WEIRDO_DIRECTION, this.serializer.getName(type, traits, BlockTraits.DIRECTION));
    }

    private void assertHorizontalEwsnDirection(BlockType type) {
        assertAll(type.toString(),
                () -> assertDirection(type, BlockTraits.DIRECTION, Direction.EAST, 0),
                () -> assertDirection(type, BlockTraits.DIRECTION, Direction.WEST, 1),
                () -> assertDirection(type, BlockTraits.DIRECTION, Direction.SOUTH, 2),
                () -> assertDirection(type, BlockTraits.DIRECTION, Direction.NORTH, 3)
        );
    }

    private void assertDirection(BlockType type, BlockTrait<Direction> trait, Direction direction, int expectedValue) {
        Map<BlockTrait<?>, Comparable<?>> traits = type.getDefaultState().getTraits();
        assertEquals(expectedValue, this.serializer.serialize(type, traits, trait, direction));
    }
}
