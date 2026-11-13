package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.data.*;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.*;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockSpecificDataTest {

    @BeforeAll
    static void initializeRegistry() {
        assertSame(BlockRegistry.get(), BlockTestRegistry.get());
    }

    @Test
    void everyDoorPreservesItsFacingHalfHingeAndOpenState() {
        for (BlockType type : BlockTypes.values()) {
            if (!type.getTraits().contains(BlockTraits.IS_DOOR_HINGE)) {
                continue;
            }

            for (BlockState state : type.getStates()) {
                Door view = state.getData(Door.class).orElseThrow();
                assertFacing(view);
                assertEquals(state.ensureTrait(BlockTraits.IS_DOOR_HINGE) ? DoorHinge.RIGHT : DoorHinge.LEFT, view.getHinge());
                assertEquals(state.ensureTrait(BlockTraits.IS_UPPER_BLOCK) ? BlockHalf.TOP : BlockHalf.BOTTOM, view.getHalf());
                assertEquals(state.ensureTrait(BlockTraits.IS_OPEN), view.isOpen());

                for (DoorHinge hinge : DoorHinge.values()) {
                    assertSame(state.withTrait(BlockTraits.IS_DOOR_HINGE, hinge == DoorHinge.RIGHT), view.withHinge(hinge));
                }

                for (BlockHalf half : BlockHalf.values()) {
                    assertSame(state.withTrait(BlockTraits.IS_UPPER_BLOCK, half == BlockHalf.TOP), view.withHalf(half));
                }

                assertSame(state.withTrait(BlockTraits.IS_OPEN, true), view.withOpen(true));
                assertSame(state.withTrait(BlockTraits.IS_OPEN, false), view.withOpen(false));
                assertThrows(NullPointerException.class, () -> view.withHinge(null));
                assertThrows(NullPointerException.class, () -> view.withHalf(null));
            }
        }

        assertTrue(BlockTypes.OAK_DOOR.getDefaultState().getData(Door.class).isPresent());
        assertTrue(BlockTypes.IRON_DOOR.getDefaultState().getData(Door.class).isPresent());
        assertTrue(BlockTypes.COPPER_DOOR.getDefaultState().getData(Door.class).isPresent());
    }

    @Test
    void everyTrapdoorUsesItsOwnVerticalHalf() {
        for (BlockType type : BlockTypes.values()) {
            if (!type.getTraits().containsAll(Set.of(BlockTraits.IS_UPSIDE_DOWN, BlockTraits.IS_OPEN, BlockTraits.DIRECTION))) {
                continue;
            }

            for (BlockState state : type.getStates()) {
                TrapDoor view = state.getData(TrapDoor.class).orElseThrow();
                assertFacing(view);
                assertEquals(state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN) ? BlockHalf.TOP : BlockHalf.BOTTOM, view.getHalf());
                assertEquals(state.ensureTrait(BlockTraits.IS_OPEN), view.isOpen());
                assertSame(state.withTrait(BlockTraits.IS_UPSIDE_DOWN, true), view.withHalf(BlockHalf.TOP));
                assertSame(state.withTrait(BlockTraits.IS_UPSIDE_DOWN, false), view.withHalf(BlockHalf.BOTTOM));
                assertSame(state.withTrait(BlockTraits.IS_OPEN, true), view.withOpen(true));
                assertSame(state.withTrait(BlockTraits.IS_OPEN, false), view.withOpen(false));
                assertThrows(NullPointerException.class, () -> view.withHalf(null));
            }
        }

        assertTrue(BlockTypes.OAK_TRAPDOOR.getDefaultState().getData(TrapDoor.class).isPresent());
        assertTrue(BlockTypes.IRON_TRAPDOOR.getDefaultState().getData(TrapDoor.class).isPresent());
    }

    @Test
    void bedPartsAndOccupiedFlagsAreIndependent() {
        for (BlockState state : BlockTypes.BED.getStates()) {
            Bed view = state.getData(Bed.class).orElseThrow();
            assertFacing(view);
            assertEquals(state.ensureTrait(BlockTraits.IS_HEAD_PIECE) ? BedPart.HEAD : BedPart.FOOT, view.getPart());
            assertEquals(state.ensureTrait(BlockTraits.IS_OCCUPIED), view.isOccupied());
            assertSame(state.withTrait(BlockTraits.IS_HEAD_PIECE, true), view.withPart(BedPart.HEAD));
            assertSame(state.withTrait(BlockTraits.IS_HEAD_PIECE, false), view.withPart(BedPart.FOOT));
            assertSame(state.withTrait(BlockTraits.IS_OCCUPIED, true), view.withOccupied(true));
            assertSame(state.withTrait(BlockTraits.IS_OCCUPIED, false), view.withOccupied(false));
            assertThrows(NullPointerException.class, () -> view.withPart(null));
        }
    }

    @Test
    void repeaterDelaysUseOneBasedRedstoneTicksAcrossPoweredVariants() {
        for (BlockType type : List.of(BlockTypes.UNPOWERED_REPEATER, BlockTypes.POWERED_REPEATER)) {
            for (BlockState state : type.getStates()) {
                Repeater view = state.getData(Repeater.class).orElseThrow();
                assertFacing(view);
                assertEquals(state.ensureTrait(BlockTraits.REPEATER_DELAY) + 1, view.getDelay());
                assertEquals(1, view.getMinimumDelay());
                assertEquals(4, view.getMaximumDelay());
                assertEquals(type == BlockTypes.POWERED_REPEATER, view.isPowered());

                for (int delay = view.getMinimumDelay(); delay <= view.getMaximumDelay(); delay++) {
                    assertSame(state.withTrait(BlockTraits.REPEATER_DELAY, delay - 1), view.withDelay(delay));
                }

                for (boolean powered : List.of(false, true)) {
                    BlockState changed = view.withPowered(powered);
                    Repeater result = changed.getData(Repeater.class).orElseThrow();
                    assertEquals(powered, result.isPowered());
                    assertEquals(view.getFacing(), result.getFacing());
                    assertEquals(view.getDelay(), result.getDelay());
                }

                assertThrows(IllegalArgumentException.class, () -> view.withDelay(0));
                assertThrows(IllegalArgumentException.class, () -> view.withDelay(5));
                assertThrows(IllegalArgumentException.class, () -> view.withDelay(Integer.MIN_VALUE));
            }
        }
    }

    @Test
    void comparatorModesPreserveOutputFlagsAndPowerChangesPreserveModes() {
        for (BlockType type : List.of(BlockTypes.UNPOWERED_COMPARATOR, BlockTypes.POWERED_COMPARATOR)) {
            for (BlockState state : type.getStates()) {
                Comparator view = state.getData(Comparator.class).orElseThrow();
                assertFacing(view);
                assertEquals(state.ensureTrait(BlockTraits.IS_OUTPUT_SUBTRACT) ? ComparatorMode.SUBTRACT : ComparatorMode.COMPARE, view.getMode());
                assertEquals(type == BlockTypes.POWERED_COMPARATOR, view.isPowered());

                for (ComparatorMode mode : ComparatorMode.values()) {
                    assertSame(state.withTrait(BlockTraits.IS_OUTPUT_SUBTRACT, mode == ComparatorMode.SUBTRACT), view.withMode(mode));
                }

                for (boolean powered : List.of(false, true)) {
                    BlockState changed = view.withPowered(powered);
                    Comparator result = changed.getData(Comparator.class).orElseThrow();
                    assertEquals(powered, result.isPowered());
                    assertEquals(view.getFacing(), result.getFacing());
                    assertEquals(view.getMode(), result.getMode());
                    assertEquals(powered, changed.ensureTrait(BlockTraits.IS_OUTPUT_LIT));
                }

                assertThrows(NullPointerException.class, () -> view.withMode(null));
            }
        }
    }

    @Test
    void farmlandMoistureRoundTripsWithoutChangingItsSource() {
        for (BlockState state : BlockTypes.FARMLAND.getStates()) {
            Farmland view = state.getData(Farmland.class).orElseThrow();
            assertEquals(state.ensureTrait(BlockTraits.MOISTURIZED_AMOUNT).intValue(), view.getMoisture());
            assertEquals(7, view.getMaximumMoisture());

            for (int moisture = 0; moisture <= view.getMaximumMoisture(); moisture++) {
                assertSame(state.withTrait(BlockTraits.MOISTURIZED_AMOUNT, moisture), view.withMoisture(moisture));
            }

            assertSame(state, view.withMoisture(view.getMoisture()));
            assertThrows(IllegalArgumentException.class, () -> view.withMoisture(-1));
            assertThrows(IllegalArgumentException.class, () -> view.withMoisture(8));
        }
    }

    @Test
    void cakeBiteCountsKeepTheFinalSliceAsAValidState() {
        for (BlockState state : BlockTypes.CAKE.getStates()) {
            Cake view = state.getData(Cake.class).orElseThrow();
            assertEquals(state.ensureTrait(BlockTraits.BITE_COUNTER).intValue(), view.getBites());
            assertEquals(6, view.getMaximumBites());

            for (int bites = 0; bites <= view.getMaximumBites(); bites++) {
                assertSame(state.withTrait(BlockTraits.BITE_COUNTER, bites), view.withBites(bites));
            }

            assertThrows(IllegalArgumentException.class, () -> view.withBites(-1));
            assertThrows(IllegalArgumentException.class, () -> view.withBites(7));
        }

        assertTrue(BlockTypes.CANDLE_CAKE.getDefaultState().getData(Cake.class).isEmpty());
    }

    @Test
    void beehiveAndNestHoneyLevelsPreserveFacing() {
        for (BlockType type : List.of(BlockTypes.BEEHIVE, BlockTypes.BEE_NEST)) {
            for (BlockState state : type.getStates()) {
                Beehive view = state.getData(Beehive.class).orElseThrow();
                assertFacing(view);
                assertEquals(state.ensureTrait(BlockTraits.HONEY_LEVEL).intValue(), view.getHoneyLevel());
                assertEquals(5, view.getMaximumHoneyLevel());

                for (int honey = 0; honey <= view.getMaximumHoneyLevel(); honey++) {
                    assertSame(state.withTrait(BlockTraits.HONEY_LEVEL, honey), view.withHoneyLevel(honey));
                }

                assertThrows(IllegalArgumentException.class, () -> view.withHoneyLevel(-1));
                assertThrows(IllegalArgumentException.class, () -> view.withHoneyLevel(6));
            }
        }
    }

    @Test
    void everyBookshelfMaskExposesAndUpdatesIndividualSlots() {
        for (BlockState state : BlockTypes.CHISELED_BOOKSHELF.getStates()) {
            ChiseledBookshelf view = state.getData(ChiseledBookshelf.class).orElseThrow();
            assertFacing(view);
            assertEquals(6, view.getSlotCount());
            int mask = state.ensureTrait(BlockTraits.BOOKS_STORED);
            Set<Integer> expectedSlots = new HashSet<>();

            for (int slot = 0; slot < view.getSlotCount(); slot++) {
                int bit = 1 << slot;
                boolean occupied = (mask & bit) != 0;
                assertEquals(occupied, view.isSlotOccupied(slot));
                if (occupied) {
                    expectedSlots.add(slot);
                }

                assertSame(state.withTrait(BlockTraits.BOOKS_STORED, mask | bit), view.withSlotOccupied(slot, true));
                assertSame(state.withTrait(BlockTraits.BOOKS_STORED, mask & ~bit), view.withSlotOccupied(slot, false));
                assertSame(state, view.withSlotOccupied(slot, occupied));
            }

            assertEquals(expectedSlots, view.getOccupiedSlots());
            assertThrows(IllegalArgumentException.class, () -> view.isSlotOccupied(-1));
            assertThrows(IllegalArgumentException.class, () -> view.isSlotOccupied(6));
            assertThrows(IllegalArgumentException.class, () -> view.withSlotOccupied(-1, true));
            assertThrows(IllegalArgumentException.class, () -> view.withSlotOccupied(Integer.MAX_VALUE, false));
        }
    }

    @Test
    void incompleteSignaturesAndUnrelatedBlocksDoNotExposeViews() {
        List<Class<? extends BlockData>> views = List.of(Door.class, TrapDoor.class, Bed.class,
                Repeater.class, Comparator.class, Farmland.class, Cake.class, Beehive.class, ChiseledBookshelf.class);

        for (Class<? extends BlockData> view : views) {
            assertTrue(BlockStates.STONE.getData(view).isEmpty());
        }

        BlockState partialDoor = BlockRegistry.get().createType(Identifier.parse("test:partial_door"),
                BlockTraits.IS_DOOR_HINGE).getDefaultState();
        assertTrue(partialDoor.getData(Door.class).isEmpty());

        BlockState unpoweredCustom = BlockRegistry.get().createType(Identifier.parse("test:delay_only"),
                BlockTraits.CARDINAL_DIRECTION, BlockTraits.REPEATER_DELAY).getDefaultState();
        assertTrue(unpoweredCustom.getData(Repeater.class).isEmpty());

        BlockState customDoor = BlockRegistry.get().createType(Identifier.parse("test:door"),
                BlockTraits.CARDINAL_DIRECTION, BlockTraits.IS_DOOR_HINGE,
                BlockTraits.IS_OPEN, BlockTraits.IS_UPPER_BLOCK).getDefaultState();
        assertTrue(customDoor.getData(Door.class).isPresent());
        assertTrue(customDoor.getData(TrapDoor.class).isEmpty());
    }

    @Test
    void blockSpecificViewsUseTheirOwnFacingTraitOnCustomBlocks() {
        BlockState door = BlockRegistry.get().createType(Identifier.parse("test:multiple_door_facings"),
                BlockTraits.CARDINAL_DIRECTION, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_OPEN,
                BlockTraits.IS_UPPER_BLOCK, BlockTraits.FACING_DIRECTION).getDefaultState()
                .withTrait(BlockTraits.FACING_DIRECTION, Direction.UP);
        Door doorView = door.getData(Door.class).orElseThrow();

        assertEquals(door.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection(), doorView.getFacing());
        assertSame(door.withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH), doorView.withFacing(Direction.NORTH));
        assertEquals(Direction.UP, doorView.withFacing(Direction.NORTH).ensureTrait(BlockTraits.FACING_DIRECTION));

        BlockState hive = BlockRegistry.get().createType(Identifier.parse("test:multiple_hive_facings"),
                BlockTraits.DIRECTION, BlockTraits.HONEY_LEVEL, BlockTraits.FACING_DIRECTION).getDefaultState()
                .withTrait(BlockTraits.FACING_DIRECTION, Direction.DOWN);
        Beehive hiveView = hive.getData(Beehive.class).orElseThrow();

        assertEquals(hive.ensureTrait(BlockTraits.DIRECTION), hiveView.getFacing());
        assertSame(hive.withTrait(BlockTraits.DIRECTION, Direction.WEST), hiveView.withFacing(Direction.WEST));
        assertEquals(Direction.DOWN, hiveView.withFacing(Direction.WEST).ensureTrait(BlockTraits.FACING_DIRECTION));
    }

    private static void assertFacing(Directional view) {
        BlockState state = view.getState();
        Directional generic = state.getData(Directional.class).orElseThrow();
        assertEquals(generic.getFacing(), view.getFacing());
        assertEquals(Set.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST), view.getFaces());

        for (Direction face : view.getFaces()) {
            assertSame(generic.withFacing(face), view.withFacing(face));
        }

        assertThrows(IllegalArgumentException.class, () -> view.withFacing(Direction.UP));
        assertThrows(NullPointerException.class, () -> view.withFacing(null));
    }
}
