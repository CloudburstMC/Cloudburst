package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.data.*;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockDataAccessTest {

    @Test
    void ageUsesBlockSpecificLimitsWithoutMutatingItsSource() {
        Ageable wheat = BlockTypes.WHEAT.getDefaultState().getData(Ageable.class).orElseThrow();
        assertEquals(7, wheat.getMaximumAge());
        assertEquals(0, wheat.getAge());

        BlockState mature = wheat.withAge(wheat.getMaximumAge());
        assertEquals(7, mature.ensureTrait(BlockTraits.GROWTH));
        assertEquals(0, wheat.getAge());
        assertSame(wheat.getState(), mature.withTrait(BlockTraits.GROWTH, 0));

        Ageable cocoa = BlockTypes.COCOA.getDefaultState().getData(Ageable.class).orElseThrow();
        assertEquals(2, cocoa.getMaximumAge());
        assertThrows(IllegalArgumentException.class, () -> cocoa.withAge(3));
        assertThrows(IllegalArgumentException.class, () -> cocoa.withAge(-1));
        assertSame(cocoa.getState().withTrait(BlockTraits.AGE, 2), cocoa.withAge(2));
    }

    @Test
    void facingAdaptsBothHorizontalAndSixWayTraits() {
        BlockState door = BlockTypes.OAK_DOOR.getDefaultState();
        Directional horizontal = door.getData(Directional.class).orElseThrow();
        assertEquals(4, horizontal.getFaces().size());
        assertThrows(IllegalArgumentException.class, () -> horizontal.withFacing(Direction.UP));
        assertThrows(UnsupportedOperationException.class, () -> horizontal.getFaces().clear());

        for (Direction facing : horizontal.getFaces()) {
            BlockState changed = horizontal.withFacing(facing);
            assertEquals(facing, changed.getData(Directional.class).orElseThrow().getFacing());
        }

        assertSame(door.withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH),
                horizontal.withFacing(Direction.NORTH));

        Directional observer = BlockTypes.OBSERVER.getDefaultState().getData(Directional.class).orElseThrow();
        assertEquals(6, observer.getFaces().size());
        assertEquals(Direction.UP, observer.withFacing(Direction.UP).ensureTrait(BlockTraits.FACING_DIRECTION));

        Directional stairs = BlockTypes.OAK_STAIRS.getDefaultState().getData(Directional.class).orElseThrow();
        assertEquals(Direction.EAST, stairs.withFacing(Direction.EAST).ensureTrait(BlockTraits.DIRECTION));
    }

    @Test
    void namedOperationsPreserveOtherTraitsAndCanonicalStates() {
        BlockState door = BlockTypes.OAK_DOOR.getDefaultState();
        BlockState north = door.getData(Directional.class).orElseThrow().withFacing(Direction.NORTH);
        Openable closed = north.getData(Openable.class).orElseThrow();
        BlockState opened = closed.withOpen(true);

        assertFalse(closed.isOpen());
        assertTrue(opened.getData(Openable.class).orElseThrow().isOpen());
        assertEquals(Direction.NORTH, opened.getData(Directional.class).orElseThrow().getFacing());
        assertSame(north, opened.getData(Openable.class).orElseThrow().withOpen(false));

        Lightable candle = BlockTypes.CANDLE.getDefaultState().getData(Lightable.class).orElseThrow();
        assertFalse(candle.isLit());
        assertTrue(candle.withLit(true).getData(Lightable.class).orElseThrow().isLit());

        Orientable log = BlockTypes.OAK_LOG.getDefaultState().getData(Orientable.class).orElseThrow();
        assertEquals(3, log.getAxes().size());
        assertThrows(UnsupportedOperationException.class, () -> log.getAxes().clear());
        assertEquals(Direction.Axis.X, log.withAxis(Direction.Axis.X).ensureTrait(BlockTraits.AXIS));
    }

    @Test
    void unsupportedViewsAreAbsentAndCustomTraitsRemainAvailable() {
        BlockState stone = BlockTypes.STONE.getDefaultState();
        List<Class<? extends BlockData>> views = List.of(Ageable.class, Directional.class,
                Orientable.class, Lightable.class, Openable.class, Powerable.class,
                AnaloguePowerable.class, Rotatable.class, Waterlogged.class);

        for (Class<? extends BlockData> view : views) {
            assertTrue(stone.getData(view).isEmpty());
        }

        assertTrue(stone.getData(BlockData.class).isEmpty());
        assertThrows(NullPointerException.class, () -> stone.getData(null));

        BlockState custom = BlockRegistry.get().createType(Identifier.parse("test:directional"),
                BlockTraits.CARDINAL_DIRECTION).getDefaultState();
        Directional facing = custom.getData(Directional.class).orElseThrow();
        assertSame(custom.withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.WEST),
                facing.withFacing(Direction.WEST));
    }

    @Test
    void lightingSupportsExtinguishedTraitsAndSeparateBlockTypes() {
        BlockState furnace = BlockTypes.FURNACE.getDefaultState()
                .withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH);
        Lightable unlit = furnace.getData(Lightable.class).orElseThrow();
        assertFalse(unlit.isLit());
        assertSame(furnace, unlit.withLit(false));

        BlockState burning = unlit.withLit(true);
        assertSame(BlockTypes.LIT_FURNACE, burning.getType());
        assertEquals(CardinalDirection.NORTH, burning.ensureTrait(BlockTraits.CARDINAL_DIRECTION));
        assertSame(furnace, burning.getData(Lightable.class).orElseThrow().withLit(false));

        BlockState campfire = BlockTypes.CAMPFIRE.getDefaultState();
        Lightable flame = campfire.getData(Lightable.class).orElseThrow();
        assertEquals(!campfire.ensureTrait(BlockTraits.IS_EXTINGUISHED), flame.isLit());
        assertTrue(flame.withLit(false).ensureTrait(BlockTraits.IS_EXTINGUISHED));
        assertFalse(flame.withLit(true).ensureTrait(BlockTraits.IS_EXTINGUISHED));
    }

    @Test
    void allExposedViewsProduceValidStates() {
        for (BlockType type : BlockTypes.values()) {
            BlockState state = type.getDefaultState();
            state.getData(Ageable.class).ifPresent(age -> {
                assertSame(state, age.getState());
                assertTrue(type.getStates().contains(age.withAge(age.getMaximumAge())));
            });
            state.getData(Directional.class).ifPresent(facing -> {
                for (Direction direction : facing.getFaces()) {
                    BlockState changed = facing.withFacing(direction);
                    assertSame(type, changed.getType());
                    assertEquals(direction, changed.getData(Directional.class).orElseThrow().getFacing());
                }
            });
            state.getData(Orientable.class).ifPresent(axis -> {
                for (Direction.Axis value : axis.getAxes()) {
                    assertTrue(type.getStates().contains(axis.withAxis(value)));
                }
            });
            state.getData(Lightable.class).ifPresent(lit -> {
                BlockState toggled = lit.withLit(!lit.isLit());
                Lightable changed = toggled.getData(Lightable.class).orElseThrow();
                assertNotEquals(lit.isLit(), changed.isLit());
                assertSame(state, changed.withLit(lit.isLit()));
            });
            state.getData(Powerable.class).ifPresent(power -> {
                BlockState changed = power.withPowered(!power.isPowered());
                assertNotEquals(power.isPowered(), changed.getData(Powerable.class).orElseThrow().isPowered());
            });
            state.getData(Rotatable.class).ifPresent(rotation -> {
                for (CardinalDirection value : rotation.getRotations()) {
                    assertEquals(value, rotation.withRotation(value).getData(Rotatable.class).orElseThrow().getRotation());
                }
            });
        }
    }

    @Test
    void coversCropStagesAndHorizontalPortalAxes() {
        Ageable pitcher = BlockTypes.PITCHER_CROP.getDefaultState().getData(Ageable.class).orElseThrow();
        assertEquals(7, pitcher.getMaximumAge());
        assertEquals(7, pitcher.withAge(7).ensureTrait(BlockTraits.GROWTH));

        Ageable torchflower = BlockTypes.TORCHFLOWER_CROP.getDefaultState().getData(Ageable.class).orElseThrow();
        assertEquals(1, torchflower.getMaximumAge());
        assertThrows(IllegalArgumentException.class, () -> torchflower.withAge(2));
        assertTrue(torchflower.getState().withTrait(BlockTraits.GROWTH, 7).getData(Ageable.class).isEmpty());

        Orientable portal = BlockTypes.PORTAL.getDefaultState().getData(Orientable.class).orElseThrow();
        assertEquals(Direction.Axis.X, portal.getAxis());
        assertEquals(Set.of(Direction.Axis.X, Direction.Axis.Z), portal.getAxes());
        assertEquals(Direction.Axis.Z, portal.withAxis(Direction.Axis.Z).ensureTrait(BlockTraits.PORTAL_AXIS));
        assertThrows(IllegalArgumentException.class, () -> portal.withAxis(Direction.Axis.Y));
        assertTrue(portal.getState().withTrait(BlockTraits.PORTAL_AXIS, Direction.Axis.Y).getData(Orientable.class).isEmpty());
    }

    @Test
    void coversBlockFacesCoralAndResolvedTorchDirections() {
        Directional crystal = BlockTypes.AMETHYST_CLUSTER.getDefaultState().getData(Directional.class).orElseThrow();
        assertEquals(6, crystal.getFaces().size());
        assertEquals(Direction.DOWN, crystal.withFacing(Direction.DOWN).ensureTrait(BlockTraits.BLOCK_FACE));

        Directional coral = BlockTypes.BRAIN_CORAL_WALL_FAN.getDefaultState().getData(Directional.class).orElseThrow();
        assertEquals(4, coral.getFaces().size());
        assertThrows(IllegalArgumentException.class, () -> coral.withFacing(Direction.UP));

        BlockState torch = BlockTypes.TORCH.getDefaultState().withTrait(BlockTraits.TORCH_DIRECTION, Direction.UP);
        Directional facing = torch.getData(Directional.class).orElseThrow();
        assertFalse(facing.getFaces().contains(Direction.DOWN));
        assertEquals(Direction.EAST, facing.withFacing(Direction.EAST).ensureTrait(BlockTraits.TORCH_DIRECTION));
        assertTrue(torch.withTrait(BlockTraits.TORCH_DIRECTION, Direction.DOWN).getData(Directional.class).isEmpty());
    }

    @Test
    void exposesBooleanAndAnaloguePowerWithoutTreatingLeversAsOpenable() {
        BlockState lever = BlockTypes.LEVER.getDefaultState();
        assertTrue(lever.getData(Openable.class).isEmpty());
        Powerable leverPower = lever.getData(Powerable.class).orElseThrow();
        assertTrue(leverPower.withPowered(true).ensureTrait(BlockTraits.IS_OPEN));

        Powerable button = BlockTypes.OAK_BUTTON.getDefaultState().getData(Powerable.class).orElseThrow();
        assertTrue(button.withPowered(true).ensureTrait(BlockTraits.IS_BUTTON_PRESSED));

        AnaloguePowerable wire = BlockTypes.REDSTONE_WIRE.getDefaultState().getData(AnaloguePowerable.class).orElseThrow();
        assertEquals(15, wire.getMaximumPower());
        assertThrows(IllegalArgumentException.class, () -> wire.withPower(16));
        assertThrows(IllegalArgumentException.class, () -> wire.withPower(-1));

        BlockState partial = wire.withPower(4);
        Powerable powered = partial.getData(Powerable.class).orElseThrow();
        assertSame(partial, powered.withPowered(true));
        assertEquals(0, powered.withPowered(false).getData(AnaloguePowerable.class).orElseThrow().getPower());
        assertEquals(15, wire.getState().getData(Powerable.class).orElseThrow().withPowered(true)
                .getData(AnaloguePowerable.class).orElseThrow().getPower());
    }

    @Test
    void poweredVariantsPreserveDirectionAndOtherSettings() {
        BlockState repeater = BlockTypes.UNPOWERED_REPEATER.getDefaultState()
                .withTrait(BlockTraits.REPEATER_DELAY, 3)
                .withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.WEST);
        BlockState powered = repeater.getData(Powerable.class).orElseThrow().withPowered(true);
        assertSame(BlockTypes.POWERED_REPEATER, powered.getType());
        assertEquals(3, powered.ensureTrait(BlockTraits.REPEATER_DELAY));
        assertEquals(CardinalDirection.WEST, powered.ensureTrait(BlockTraits.CARDINAL_DIRECTION));
        assertSame(repeater, powered.getData(Powerable.class).orElseThrow().withPowered(false));

        BlockState comparator = BlockTypes.UNPOWERED_COMPARATOR.getDefaultState();
        BlockState active = comparator.getData(Powerable.class).orElseThrow().withPowered(true);
        assertSame(BlockTypes.POWERED_COMPARATOR, active.getType());
        assertTrue(active.ensureTrait(BlockTraits.IS_OUTPUT_LIT));
        assertSame(comparator, active.getData(Powerable.class).orElseThrow().withPowered(false));
    }

    @Test
    void exposesAllSixteenSignRotationsWithoutRounding() {
        Rotatable sign = BlockTypes.OAK_STANDING_SIGN.getDefaultState().getData(Rotatable.class).orElseThrow();
        assertEquals(16, sign.getRotations().size());
        assertThrows(UnsupportedOperationException.class, () -> sign.getRotations().clear());

        for (CardinalDirection rotation : sign.getRotations()) {
            BlockState changed = sign.withRotation(rotation);
            assertEquals(rotation, changed.ensureTrait(BlockTraits.SIGN_DIRECTION));
            assertSame(sign.getState(), changed.getData(Rotatable.class).orElseThrow().withRotation(sign.getRotation()));
        }
    }
}
