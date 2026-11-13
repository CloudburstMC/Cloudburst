package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.*;
import org.cloudburstmc.server.block.CloudBlockSnapshot;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockTransformTest {

    @BeforeAll
    static void initializeRegistry() {
        assertSame(BlockRegistry.get(), BlockTestRegistry.get());
    }

    @Test
    void supportedCatalogStatesObeyRotationAndReflectionIdentities() {
        int checked = 0;
        for (BlockType type : BlockTypes.values()) {
            if (type.getTraits().contains(BlockTraits.JIGSAW_ROTATION)
                    || type.getTraits().contains(BlockTraits.CHALKBOARD_DIRECTION)
                    || type.getTraits().contains(BlockTraits.CHEMISTRY_TABLE_DIRECTION)) {
                continue;
            }

            for (BlockState state : type.getStates()) {
                assertSame(state, state.rotate(BlockRotation.NONE));
                assertSame(state, state.mirror(BlockMirror.NONE));

                BlockState quarter = state.rotate(BlockRotation.CLOCKWISE_90);
                assertSame(state, quarter.rotate(BlockRotation.COUNTERCLOCKWISE_90));
                assertSame(state, quarter.rotate(BlockRotation.CLOCKWISE_90)
                        .rotate(BlockRotation.CLOCKWISE_90).rotate(BlockRotation.CLOCKWISE_90));
                assertSame(state, state.rotate(BlockRotation.CLOCKWISE_180).rotate(BlockRotation.CLOCKWISE_180));
                assertSame(state.rotate(BlockRotation.CLOCKWISE_180),
                        state.mirror(BlockMirror.LEFT_RIGHT).mirror(BlockMirror.FRONT_BACK));

                for (BlockMirror mirror : List.of(BlockMirror.LEFT_RIGHT, BlockMirror.FRONT_BACK)) {
                    assertSame(state, state.mirror(mirror).mirror(mirror));
                    assertSame(state.rotate(BlockRotation.COUNTERCLOCKWISE_90),
                            state.mirror(mirror).rotate(BlockRotation.CLOCKWISE_90).mirror(mirror));
                }

                checked++;
            }
        }

        assertTrue(checked > 10000, "Exercise the native state catalog, not just default states");
    }

    @Test
    void directionsAxesAndSignSegmentsUseWorldCoordinates() {
        BlockState facing = state("transform_facing", BlockTraits.FACING_DIRECTION);
        Map<Direction, Direction> clockwise = Map.of(
                Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH,
                Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.NORTH,
                Direction.UP, Direction.UP, Direction.DOWN, Direction.DOWN);

        for (Direction direction : Direction.values()) {
            BlockState original = facing.withTrait(BlockTraits.FACING_DIRECTION, direction);
            assertEquals(clockwise.get(direction), original.rotate(BlockRotation.CLOCKWISE_90)
                    .ensureTrait(BlockTraits.FACING_DIRECTION));
            assertEquals(direction == Direction.NORTH || direction == Direction.SOUTH ? direction.getOpposite() : direction,
                    original.mirror(BlockMirror.LEFT_RIGHT).ensureTrait(BlockTraits.FACING_DIRECTION));
            assertEquals(direction, original.ensureTrait(BlockTraits.FACING_DIRECTION));
        }

        BlockState axis = state("transform_axis", BlockTraits.AXIS);
        assertEquals(Direction.Axis.Z, axis.withTrait(BlockTraits.AXIS, Direction.Axis.X)
                .rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.AXIS));
        assertEquals(Direction.Axis.Y, axis.withTrait(BlockTraits.AXIS, Direction.Axis.Y)
                .rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.AXIS));

        BlockState sign = state("transform_sign", BlockTraits.SIGN_DIRECTION);
        CardinalDirection[] segments = CardinalDirection.values();
        for (int index = 0; index < segments.length; index++) {
            BlockState original = sign.withTrait(BlockTraits.SIGN_DIRECTION, segments[index]);
            assertEquals(segments[(index + 4) % 16], original.rotate(BlockRotation.CLOCKWISE_90)
                    .ensureTrait(BlockTraits.SIGN_DIRECTION));
            assertEquals(segments[(24 - index) % 16], original.mirror(BlockMirror.LEFT_RIGHT)
                    .ensureTrait(BlockTraits.SIGN_DIRECTION));
            assertEquals(segments[(16 - index) % 16], original.mirror(BlockMirror.FRONT_BACK)
                    .ensureTrait(BlockTraits.SIGN_DIRECTION));
        }
    }

    @Test
    void railsLeversAndCraftersRetainTheirAttachmentGeometry() {
        BlockState rail = state("transform_rail", BlockTraits.RAIL_DIRECTION);
        assertEquals(RailDirection.ASCENDING_SOUTH, rail.withTrait(BlockTraits.RAIL_DIRECTION, RailDirection.ASCENDING_EAST)
                .rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.RAIL_DIRECTION));
        assertEquals(RailDirection.NORTH_EAST, rail.withTrait(BlockTraits.RAIL_DIRECTION, RailDirection.SOUTH_EAST)
                .mirror(BlockMirror.LEFT_RIGHT).ensureTrait(BlockTraits.RAIL_DIRECTION));
        assertEquals(RailDirection.EAST_WEST, rail.withTrait(BlockTraits.RAIL_DIRECTION, RailDirection.NORTH_SOUTH)
                .rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.RAIL_DIRECTION));

        BlockState lever = state("transform_lever", BlockTraits.LEVER_DIRECTION);
        assertEquals(LeverDirection.UP_EAST_WEST, lever.withTrait(BlockTraits.LEVER_DIRECTION, LeverDirection.UP_NORTH_SOUTH)
                .rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.LEVER_DIRECTION));
        assertEquals(LeverDirection.SOUTH, lever.withTrait(BlockTraits.LEVER_DIRECTION, LeverDirection.NORTH)
                .mirror(BlockMirror.LEFT_RIGHT).ensureTrait(BlockTraits.LEVER_DIRECTION));

        BlockState crafter = state("transform_crafter", BlockTraits.CRAFTER_ORIENTATION);
        assertEquals(CrafterOrientation.DOWN_EAST, crafter.withTrait(BlockTraits.CRAFTER_ORIENTATION, CrafterOrientation.DOWN_NORTH)
                .rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.CRAFTER_ORIENTATION));
        assertEquals(CrafterOrientation.SOUTH_UP, crafter.withTrait(BlockTraits.CRAFTER_ORIENTATION, CrafterOrientation.NORTH_UP)
                .mirror(BlockMirror.LEFT_RIGHT).ensureTrait(BlockTraits.CRAFTER_ORIENTATION));
    }

    @Test
    void connectionsArePermutedWithoutOverwritingSourceValues() {
        BlockState wall = state("transform_wall", BlockTraits.WALL_CONNECTION_NORTH, BlockTraits.WALL_CONNECTION_EAST,
                BlockTraits.WALL_CONNECTION_SOUTH, BlockTraits.WALL_CONNECTION_WEST)
                .withTrait(BlockTraits.WALL_CONNECTION_NORTH, WallConnectionType.TALL)
                .withTrait(BlockTraits.WALL_CONNECTION_EAST, WallConnectionType.SHORT)
                .withTrait(BlockTraits.WALL_CONNECTION_SOUTH, WallConnectionType.NONE)
                .withTrait(BlockTraits.WALL_CONNECTION_WEST, WallConnectionType.TALL);
        BlockState rotated = wall.rotate(BlockRotation.CLOCKWISE_90);
        assertEquals(WallConnectionType.TALL, rotated.ensureTrait(BlockTraits.WALL_CONNECTION_EAST));
        assertEquals(WallConnectionType.SHORT, rotated.ensureTrait(BlockTraits.WALL_CONNECTION_SOUTH));
        assertEquals(WallConnectionType.NONE, rotated.ensureTrait(BlockTraits.WALL_CONNECTION_WEST));
        assertEquals(WallConnectionType.TALL, rotated.ensureTrait(BlockTraits.WALL_CONNECTION_NORTH));

        BlockState wire = state("transform_wire", BlockTraits.REDSTONE_NORTH, BlockTraits.REDSTONE_EAST,
                BlockTraits.REDSTONE_SOUTH, BlockTraits.REDSTONE_WEST)
                .withTrait(BlockTraits.REDSTONE_NORTH, RedstoneSide.UP);
        assertEquals(RedstoneSide.UP, wire.rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.REDSTONE_EAST));

        BlockState faces = state("transform_faces", BlockTraits.CONNECTION_UP, BlockTraits.CONNECTION_DOWN,
                BlockTraits.CONNECTION_NORTH, BlockTraits.CONNECTION_SOUTH, BlockTraits.CONNECTION_EAST, BlockTraits.CONNECTION_WEST)
                .withTrait(BlockTraits.CONNECTION_UP, true).withTrait(BlockTraits.CONNECTION_NORTH, true);
        BlockState reflected = faces.mirror(BlockMirror.LEFT_RIGHT);
        assertTrue(reflected.ensureTrait(BlockTraits.CONNECTION_UP));
        assertTrue(reflected.ensureTrait(BlockTraits.CONNECTION_SOUTH));
        assertFalse(reflected.ensureTrait(BlockTraits.CONNECTION_NORTH));

        BlockState multi = state("transform_multiface", BlockTraits.MULTI_FACE_DIRECTION)
                .withTrait(BlockTraits.MULTI_FACE_DIRECTION, 1 | 2 | 16);
        assertEquals(1 | 2 | 32, multi.rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.MULTI_FACE_DIRECTION));

        BlockState vine = state("transform_vine", BlockTraits.VINE_DIRECTION_BITS)
                .withTrait(BlockTraits.VINE_DIRECTION_BITS, 4 | 8);
        assertEquals(8 | 1, vine.rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.VINE_DIRECTION_BITS));
    }

    @Test
    void reflectionsReverseHandedPropertiesButNotOtherValues() {
        BlockState door = BlockTypes.OAK_DOOR.getDefaultState()
                .withTrait(BlockTraits.IS_DOOR_HINGE, true).withTrait(BlockTraits.IS_OPEN, true);
        for (BlockMirror mirror : List.of(BlockMirror.LEFT_RIGHT, BlockMirror.FRONT_BACK)) {
            assertFalse(door.mirror(mirror).ensureTrait(BlockTraits.IS_DOOR_HINGE));
            assertTrue(door.mirror(mirror).ensureTrait(BlockTraits.IS_OPEN));
        }

        BlockState stair = state("transform_stair", BlockTraits.STAIR_SHAPE);
        assertEquals(StairShape.INNER_RIGHT, stair.withTrait(BlockTraits.STAIR_SHAPE, StairShape.INNER_LEFT)
                .mirror(BlockMirror.FRONT_BACK).ensureTrait(BlockTraits.STAIR_SHAPE));
        assertEquals(StairShape.OUTER_RIGHT, stair.withTrait(BlockTraits.STAIR_SHAPE, StairShape.OUTER_LEFT)
                .mirror(BlockMirror.LEFT_RIGHT).ensureTrait(BlockTraits.STAIR_SHAPE));

        BlockState shelf = state("transform_shelf", BlockTraits.POWERED_SHELF_TYPE)
                .withTrait(BlockTraits.POWERED_SHELF_TYPE, 1);
        assertEquals(3, shelf.mirror(BlockMirror.FRONT_BACK).ensureTrait(BlockTraits.POWERED_SHELF_TYPE));
        assertEquals(1, shelf.rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.POWERED_SHELF_TYPE));
    }

    @Test
    void mushroomTexturesAndCoralAxesTransformWithoutLosingSpecialValues() {
        BlockState mushroom = state("transform_mushroom", BlockTraits.HUGE_MUSHROOM_BITS);
        assertEquals(3, mushroom.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 1)
                .rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.HUGE_MUSHROOM_BITS));
        assertEquals(7, mushroom.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, 1)
                .mirror(BlockMirror.LEFT_RIGHT).ensureTrait(BlockTraits.HUGE_MUSHROOM_BITS));
        for (int value : List.of(0, 5, 10, 11, 12, 13, 14, 15)) {
            BlockState original = mushroom.withTrait(BlockTraits.HUGE_MUSHROOM_BITS, value);
            assertSame(original, original.rotate(BlockRotation.CLOCKWISE_90));
            assertSame(original, original.mirror(BlockMirror.FRONT_BACK));
        }

        BlockState coral = state("transform_coral", BlockTraits.CORAL_FAN_DIRECTION)
                .withTrait(BlockTraits.CORAL_FAN_DIRECTION, 0);
        assertEquals(1, coral.rotate(BlockRotation.CLOCKWISE_90).ensureTrait(BlockTraits.CORAL_FAN_DIRECTION));
        assertSame(coral, coral.mirror(BlockMirror.LEFT_RIGHT));
    }

    @Test
    void snapshotsTransformBothLayersWithoutChangingTheirContents() {
        BlockState facing = state("transform_snapshot", BlockTraits.FACING_DIRECTION)
                .withTrait(BlockTraits.FACING_DIRECTION, Direction.NORTH);
        BlockSnapshot waterlogged = new CloudBlockSnapshot(facing, BlockTypes.WATER.getDefaultState());
        BlockSnapshot rotated = waterlogged.rotate(BlockRotation.CLOCKWISE_90);
        assertEquals(Direction.EAST, rotated.getState(BlockLayer.PRIMARY).ensureTrait(BlockTraits.FACING_DIRECTION));
        assertSame(waterlogged.getState(BlockLayer.SECONDARY), rotated.getState(BlockLayer.SECONDARY));
        assertSame(facing, waterlogged.getState(BlockLayer.PRIMARY));

        BlockSnapshot layered = new CloudBlockSnapshot(BlockStates.STONE, facing);
        assertSame(BlockStates.STONE, layered.mirror(BlockMirror.LEFT_RIGHT).getState(BlockLayer.PRIMARY));
        assertEquals(Direction.SOUTH, layered.mirror(BlockMirror.LEFT_RIGHT).getState(BlockLayer.SECONDARY)
                .ensureTrait(BlockTraits.FACING_DIRECTION));
        assertSame(layered, layered.rotate(BlockRotation.NONE));
        assertThrows(NullPointerException.class, () -> layered.rotate(null));
        assertThrows(NullPointerException.class, () -> layered.mirror(null));
    }

    @Test
    void invalidCustomDomainsAndUnverifiedEncodingsFailExplicitly() {
        EnumBlockTrait<Direction> restricted = EnumBlockTrait.of("test:restricted_facing", Direction.class,
                Set.of(Direction.NORTH), Direction.NORTH);
        BlockState limited = state("transform_limited", restricted);
        assertThrows(IllegalArgumentException.class, () -> limited.rotate(BlockRotation.CLOCKWISE_90));
        assertEquals(Direction.NORTH, limited.ensureTrait(restricted));

        BlockState partial = state("transform_partial_faces", BlockTraits.CONNECTION_NORTH);
        assertThrows(IllegalArgumentException.class, () -> partial.rotate(BlockRotation.CLOCKWISE_90));

        for (BlockTrait<?> trait : List.of(BlockTraits.JIGSAW_ROTATION, BlockTraits.CHALKBOARD_DIRECTION,
                BlockTraits.CHEMISTRY_TABLE_DIRECTION)) {
            BlockState encoded = state("transform_encoded_" + trait.getName().getName(), trait);
            assertSame(encoded, encoded.rotate(BlockRotation.NONE));
            assertThrows(UnsupportedOperationException.class, () -> encoded.rotate(BlockRotation.CLOCKWISE_90));
            assertThrows(UnsupportedOperationException.class, () -> encoded.mirror(BlockMirror.FRONT_BACK));
        }

        assertSame(BlockStates.STONE, BlockStates.STONE.rotate(BlockRotation.CLOCKWISE_90));
        assertThrows(NullPointerException.class, () -> BlockStates.STONE.rotate(null));
        assertThrows(NullPointerException.class, () -> BlockStates.STONE.mirror(null));
    }

    private static BlockState state(String name, BlockTrait<?>... traits) {
        return BlockRegistry.get().createType(Identifier.parse("test:" + name), traits).getDefaultState();
    }
}
