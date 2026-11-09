package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.data.*;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.*;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockStructureDataTest {

    private static final BlockRegistry BLOCKS = BlockTestRegistry.get();

    @Test
    void slabFormsUseRegisteredFamiliesIncludingCopperNaming() {
        assertSame(BlockTypes.OAK_SLAB, BLOCKS.getOrThrow(Identifier.parse("minecraft:oak_slab")));
        Slab oak = BlockTypes.OAK_SLAB.getDefaultState().getData(Slab.class).orElseThrow();
        assertEquals(SlabType.BOTTOM, oak.getType());
        assertSame(oak.getState(), oak.withType(SlabType.BOTTOM));

        BlockState top = oak.withType(SlabType.TOP);
        assertEquals(SlabSlot.TOP, top.ensureTrait(BlockTraits.SLAB_SLOT));
        BlockState doubled = top.getData(Slab.class).orElseThrow().withType(SlabType.DOUBLE);
        assertSame(BlockTypes.OAK_DOUBLE_SLAB, doubled.getType());
        assertEquals(SlabType.DOUBLE, doubled.getData(Slab.class).orElseThrow().getType());
        assertSame(top, doubled.getData(Slab.class).orElseThrow().withType(SlabType.TOP));

        Slab copper = BlockTypes.CUT_COPPER_SLAB.getDefaultState().getData(Slab.class).orElseThrow();
        assertSame(BlockTypes.DOUBLE_CUT_COPPER_SLAB, copper.withType(SlabType.DOUBLE).getType());
    }

    @Test
    void everyRegisteredSlabHasBothFormsAndEveryStairSupportsItsShapes() {
        for (BlockType type : BlockTypes.values()) {
            if (type.getTraits().contains(BlockTraits.SLAB_SLOT)) {
                Slab slab = type.getDefaultState().getData(Slab.class).orElseThrow(
                        () -> new AssertionError("Missing slab family: " + type.getId()));
                for (SlabType value : SlabType.values()) {
                    BlockState changed = slab.withType(value);
                    assertEquals(value, changed.getData(Slab.class).orElseThrow().getType());
                }
            }

            if (type.getTraits().contains(BlockTraits.STAIR_SHAPE)) {
                Stairs stairs = type.getDefaultState().getData(Stairs.class).orElseThrow();
                assertEquals(StairShape.STRAIGHT, stairs.getShape());
                for (StairShape shape : StairShape.values()) {
                    assertEquals(shape, stairs.withShape(shape).getData(Stairs.class).orElseThrow().getShape());
                }
            }
        }
    }

    @Test
    void stairOperationsRetainHalfFacingAndCornerProperties() {
        BlockState stair = BlockTypes.OAK_STAIRS.getDefaultState()
                .withTrait(BlockTraits.DIRECTION, Direction.WEST)
                .withTrait(BlockTraits.STAIR_SHAPE, StairShape.INNER_LEFT);
        Stairs view = stair.getData(Stairs.class).orElseThrow();
        BlockState upsideDown = view.withHalf(BlockHalf.TOP);
        Stairs changed = upsideDown.getData(Stairs.class).orElseThrow();
        assertEquals(BlockHalf.TOP, changed.getHalf());
        assertEquals(Direction.WEST, changed.getFacing());
        assertEquals(StairShape.INNER_LEFT, changed.getShape());
        assertSame(stair, changed.withHalf(BlockHalf.BOTTOM));
        assertThrows(IllegalArgumentException.class, () -> view.withFacing(Direction.UP));
    }

    @Test
    void halvesCoverDoorsPlantsAndTallSeagrassWithoutConvertingSinglePlants() {
        Bisected door = BlockTypes.OAK_DOOR.getDefaultState().getData(Bisected.class).orElseThrow();
        assertEquals(BlockHalf.TOP, door.withHalf(BlockHalf.TOP).getData(Bisected.class).orElseThrow().getHalf());
        assertFalse(door.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK));

        BlockState single = BlockTypes.SEAGRASS.getDefaultState();
        assertTrue(single.getData(Bisected.class).isEmpty());
        BlockState tall = single.withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_BOTTOM);
        BlockState top = tall.getData(Bisected.class).orElseThrow().withHalf(BlockHalf.TOP);
        assertEquals(SeaGrassType.DOUBLE_TOP, top.ensureTrait(BlockTraits.SEA_GRASS_TYPE));
        assertSame(tall, top.getData(Bisected.class).orElseThrow().withHalf(BlockHalf.BOTTOM));
    }

    @Test
    void railsExposeOnlyTheShapesTheirTraitAllows() {
        Rail regular = BlockTypes.RAIL.getDefaultState().getData(Rail.class).orElseThrow();
        Rail powered = BlockTypes.GOLDEN_RAIL.getDefaultState().getData(Rail.class).orElseThrow();
        assertEquals(10, regular.getShapes().size());
        assertEquals(6, powered.getShapes().size());
        assertThrows(IllegalArgumentException.class, () -> powered.withShape(RailDirection.NORTH_EAST));
        assertThrows(UnsupportedOperationException.class, () -> regular.getShapes().clear());

        for (BlockType type : BlockTypes.values()) {
            type.getDefaultState().getData(Rail.class).ifPresent(rail -> {
                for (RailDirection shape : rail.getShapes()) {
                    assertEquals(shape, rail.withShape(shape).getData(Rail.class).orElseThrow().getShape());
                }
            });
        }
    }

    @Test
    void attachmentChangesPreserveLeverAlignmentPowerAndDoubleWallMounts() {
        BlockState lever = BlockTypes.LEVER.getDefaultState()
                .withTrait(BlockTraits.LEVER_DIRECTION, LeverDirection.UP_EAST_WEST)
                .withTrait(BlockTraits.IS_OPEN, true);
        FaceAttachable floor = lever.getData(FaceAttachable.class).orElseThrow();
        assertEquals(AttachedFace.FLOOR, floor.getAttachedFace());
        BlockState ceiling = floor.withAttachedFace(AttachedFace.CEILING);
        assertEquals(LeverDirection.DOWN_EAST_WEST, ceiling.ensureTrait(BlockTraits.LEVER_DIRECTION));
        assertTrue(ceiling.ensureTrait(BlockTraits.IS_OPEN));

        BlockState bell = BlockTypes.BELL.getDefaultState().withTrait(BlockTraits.ATTACHMENT, AttachmentType.MULTIPLE);
        FaceAttachable wall = bell.getData(FaceAttachable.class).orElseThrow();
        assertEquals(AttachedFace.WALL, wall.getAttachedFace());
        assertSame(bell, wall.withAttachedFace(AttachedFace.WALL));

        for (BlockType type : Set.of(BlockTypes.OAK_BUTTON, BlockTypes.LEVER, BlockTypes.GRINDSTONE, BlockTypes.BELL)) {
            FaceAttachable view = type.getDefaultState().getData(FaceAttachable.class).orElseThrow();
            for (AttachedFace face : AttachedFace.values()) {
                assertEquals(face, view.withAttachedFace(face).getData(FaceAttachable.class).orElseThrow().getAttachedFace());
            }
        }
    }
}
