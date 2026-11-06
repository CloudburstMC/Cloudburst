package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.data.MultipleFacing;
import org.cloudburstmc.api.block.data.RedstoneWire;
import org.cloudburstmc.api.block.data.Wall;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.RedstoneSide;
import org.cloudburstmc.api.util.data.WallConnectionType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockConnectionsDataTest {

    @Test
    void booleanConnectionsExposeOnlyTheirSupportedFaces() {
        MultipleFacing pane = BlockTypes.GLASS_PANE.getDefaultState().getData(MultipleFacing.class).orElseThrow();
        assertEquals(Set.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST), pane.getAllowedFaces());
        assertThrows(IllegalArgumentException.class, () -> pane.hasFace(Direction.UP));
        assertThrows(IllegalArgumentException.class, () -> pane.withFace(Direction.DOWN, true));
        assertThrows(UnsupportedOperationException.class, () -> pane.getAllowedFaces().clear());

        BlockState north = pane.withFace(Direction.NORTH, true);
        assertTrue(north.getData(MultipleFacing.class).orElseThrow().hasFace(Direction.NORTH));
        assertFalse(pane.hasFace(Direction.NORTH));
        assertSame(pane.getState(), north.getData(MultipleFacing.class).orElseThrow().withFace(Direction.NORTH, false));
    }

    @Test
    void sixFaceMasksUseBedrockBitOrderAndRetainOtherFaces() {
        Map<Direction, Integer> expected = Map.of(Direction.DOWN, 1, Direction.UP, 2,
                Direction.SOUTH, 4, Direction.WEST, 8, Direction.NORTH, 16, Direction.EAST, 32);
        MultipleFacing lichen = BlockTypes.GLOW_LICHEN.getDefaultState().getData(MultipleFacing.class).orElseThrow();
        assertEquals(6, lichen.getAllowedFaces().size());

        for (Map.Entry<Direction, Integer> entry : expected.entrySet()) {
            BlockState changed = lichen.withFace(entry.getKey(), true);
            assertEquals(entry.getValue(), changed.ensureTrait(BlockTraits.MULTI_FACE_DIRECTION));
        }

        BlockState all = lichen.getState().withTrait(BlockTraits.MULTI_FACE_DIRECTION, 63);
        MultipleFacing faces = all.getData(MultipleFacing.class).orElseThrow();
        assertEquals(6, faces.getFaces().size());
        assertThrows(UnsupportedOperationException.class, () -> faces.getFaces().clear());
        BlockState withoutNorth = faces.withFace(Direction.NORTH, false);
        assertEquals(47, withoutNorth.ensureTrait(BlockTraits.MULTI_FACE_DIRECTION));
        assertSame(all, withoutNorth.getData(MultipleFacing.class).orElseThrow().withFace(Direction.NORTH, true));
    }

    @Test
    void vineMasksAreHorizontalAndUseTheirOwnBitOrder() {
        MultipleFacing vine = BlockTypes.VINE.getDefaultState().getData(MultipleFacing.class).orElseThrow();
        assertEquals(4, vine.getAllowedFaces().size());
        assertThrows(IllegalArgumentException.class, () -> vine.withFace(Direction.UP, true));
        assertEquals(1, vine.withFace(Direction.SOUTH, true).ensureTrait(BlockTraits.VINE_DIRECTION_BITS));
        assertEquals(2, vine.withFace(Direction.WEST, true).ensureTrait(BlockTraits.VINE_DIRECTION_BITS));
        assertEquals(4, vine.withFace(Direction.NORTH, true).ensureTrait(BlockTraits.VINE_DIRECTION_BITS));
        assertEquals(8, vine.withFace(Direction.EAST, true).ensureTrait(BlockTraits.VINE_DIRECTION_BITS));
    }

    @Test
    void wallHeightsAndWireShapesRemainIndependent() {
        Wall wall = BlockTypes.COBBLESTONE_WALL.getDefaultState().getData(Wall.class).orElseThrow();
        BlockState tall = wall.withHeight(Direction.NORTH, WallConnectionType.TALL);
        assertEquals(WallConnectionType.TALL, tall.getData(Wall.class).orElseThrow().getHeight(Direction.NORTH));
        assertEquals(wall.isUp(), tall.getData(Wall.class).orElseThrow().isUp());
        assertThrows(IllegalArgumentException.class, () -> wall.getHeight(Direction.UP));
        assertTrue(wall.withUp(true).getData(Wall.class).orElseThrow().isUp());

        RedstoneWire wire = BlockTypes.REDSTONE_WIRE.getDefaultState().getData(RedstoneWire.class).orElseThrow();
        BlockState powered = wire.withPower(9);
        BlockState climbed = powered.getData(RedstoneWire.class).orElseThrow().withFace(Direction.EAST, RedstoneSide.UP);
        RedstoneWire changed = climbed.getData(RedstoneWire.class).orElseThrow();
        assertEquals(9, changed.getPower());
        assertEquals(RedstoneSide.UP, changed.getFace(Direction.EAST));
        assertThrows(IllegalArgumentException.class, () -> wire.withFace(Direction.DOWN, RedstoneSide.SIDE));
        assertThrows(IllegalArgumentException.class, () -> wire.withPower(16));
    }

    @Test
    void everyBooleanFaceCanBeChangedWithoutChangingOtherFaces() {
        for (BlockType type : BlockTypes.values()) {
            type.getDefaultState().getData(MultipleFacing.class).ifPresent(view -> {
                for (Direction face : view.getAllowedFaces()) {
                    boolean original = view.hasFace(face);
                    BlockState changed = view.withFace(face, !original);
                    MultipleFacing result = changed.getData(MultipleFacing.class).orElseThrow();
                    assertNotEquals(original, result.hasFace(face));
                    for (Direction other : view.getAllowedFaces()) {
                        if (other != face) {
                            assertEquals(view.hasFace(other), result.hasFace(other));
                        }
                    }
                    assertSame(view.getState(), result.withFace(face, original));
                }
            });
        }
    }
}
