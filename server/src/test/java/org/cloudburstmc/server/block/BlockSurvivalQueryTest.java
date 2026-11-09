package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.SeaGrassType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockSurvivalQueryTest {

    private static final Vector3i POSITION = Vector3i.from(3, 70, -5);

    @Test
    void proposedStateUsesItsOwnSurvivalRulesWithoutReplacingTheTarget() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.STONE);
        BlockState plant = BlockTypes.DANDELION.getDefaultState();
        assertFalse(plant.isSupported(world.block(POSITION)));

        world.setState(POSITION.add(0, -1, 0), BlockTypes.DIRT.getDefaultState());
        assertTrue(plant.isSupported(world.block(POSITION)));
        assertSame(BlockStates.STONE, world.state(POSITION));
        assertTrue(world.getEvents(Event.class).isEmpty());
        assertTrue(world.getScheduledDelays().isEmpty());
        assertTrue(world.getDrops().isEmpty());
        assertEquals(0, world.getBreakCount());
    }

    @Test
    void waterPlantsUseExistingWaterWithoutInventingIt() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        BlockState kelp = BlockTypes.KELP.getDefaultState();
        assertFalse(kelp.isSupported(world.block(POSITION)));

        world.setState(POSITION, BlockStates.WATER);
        assertTrue(kelp.isSupported(world.block(POSITION)));
        assertSame(BlockStates.WATER, world.state(POSITION));

        BlockSnapshot dry = new CloudBlockSnapshot(kelp, BlockStates.AIR);
        BlockSnapshot wet = new CloudBlockSnapshot(kelp, BlockStates.WATER);
        assertFalse(dry.isSupported(world.block(POSITION)));
        assertTrue(wet.isSupported(world.block(POSITION)));

        world.setState(POSITION, BlockTypes.OAK_STAIRS.getDefaultState());
        world.setLiquid(POSITION, BlockStates.WATER.getLiquidState());
        assertTrue(kelp.isSupported(world.block(POSITION)));
        assertSame(BlockTypes.OAK_STAIRS, world.state(POSITION).getType());
    }

    @Test
    void snapshotsUseExactLayersAndCheckSnowloggedPlants() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockSnapshot invalid = new CloudBlockSnapshot(BlockStates.STONE, BlockStates.WATER);
        assertFalse(invalid.isSupported(world.block(POSITION)));

        BlockSnapshot snow = new CloudBlockSnapshot(BlockTypes.SNOW_LAYER.getDefaultState(),
                BlockTypes.DANDELION.getDefaultState());
        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        assertFalse(snow.isSupported(world.block(POSITION)));

        world.setState(POSITION.add(0, -1, 0), BlockTypes.DIRT.getDefaultState());
        assertTrue(snow.isSupported(world.block(POSITION)));
        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void hangingDripstoneUsesTheProposedOrientation() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockState dripstone = BlockTypes.POINTED_DRIPSTONE.getDefaultState();
        world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
        assertTrue(dripstone.withTrait(BlockTraits.IS_HANGING, true).isSupported(world.block(POSITION)));
        assertFalse(dripstone.withTrait(BlockTraits.IS_HANGING, false).isSupported(world.block(POSITION)));
    }

    @Test
    void torchesUseTheirOwnFloorOrWallSupport() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockState floor = BlockTypes.TORCH.getDefaultState().withTrait(BlockTraits.TORCH_DIRECTION, Direction.UP);
        BlockState wall = floor.withTrait(BlockTraits.TORCH_DIRECTION, Direction.WEST);
        assertFalse(floor.isSupported(world.block(POSITION)));
        assertFalse(wall.isSupported(world.block(POSITION)));

        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        assertTrue(floor.isSupported(world.block(POSITION)));
        assertFalse(floor.withTrait(BlockTraits.TORCH_DIRECTION, Direction.DOWN).isSupported(world.block(POSITION)));
        assertFalse(wall.isSupported(world.block(POSITION)));

        world.setState(Direction.WEST.relative(POSITION), BlockStates.STONE);
        assertTrue(wall.isSupported(world.block(POSITION)));
        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void floorTorchPlacementUsesTheNativeTopOrientation() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        Player player = InterfaceProxy.create(Player.class, Map.of("getLevel", world.block(POSITION).getLevel()));
        BlockState resolved = BlockTestRegistry.get()
                .requireComponent(BlockTypes.TORCH, BlockComponents.RESOLVE_PLACEMENT_STATE)
                .execute(BlockTypes.TORCH.getDefaultState(), world.block(POSITION), player, Direction.UP, Vector3f.ZERO);
        assertNotNull(resolved);
        Block proposed = new CloudBlock(player.getLevel(), POSITION, new BlockState[]{resolved, BlockStates.AIR});
        assertTrue(proposed.requireComponent(BlockComponents.CAN_BE_PLACED).execute(proposed));
        boolean placed = BlockTestRegistry.get().requireComponent(BlockTypes.TORCH, BlockComponents.ON_PLACE)
                .execute(resolved, player, POSITION, Direction.UP, Vector3f.ZERO);
        assertTrue(placed);
        assertEquals(Direction.UP, world.state(POSITION).ensureTrait(BlockTraits.TORCH_DIRECTION));
        assertTrue(world.state(POSITION).isSupported(world.block(POSITION)));
    }

    @Test
    void upperPlantHalvesRequireTheMatchingLowerHalf() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockState grass = BlockTypes.SEAGRASS.getDefaultState();
        BlockSnapshot upper = new CloudBlockSnapshot(grass.withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_TOP),
                BlockStates.WATER);
        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        assertFalse(upper.isSupported(world.block(POSITION)));

        world.setState(POSITION.add(0, -1, 0), grass.withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_BOTTOM));
        assertTrue(upper.isSupported(world.block(POSITION)));
    }

    @Test
    void buildLimitsAreCheckedBeforeWorldAccess() {
        BlockState stone = BlockStates.STONE;
        BlockSnapshot snapshot = new CloudBlockSnapshot(stone, BlockStates.AIR);
        Vector3i below = Vector3i.from(0, -65, 0);
        Vector3i above = Vector3i.from(0, 320, 0);
        assertFalse(stone.isSupported(TestLevels.overworld(), below));
        assertFalse(snapshot.isSupported(TestLevels.overworld(), above));
        assertThrows(NullPointerException.class, () -> stone.isSupported(null, POSITION));
        assertThrows(NullPointerException.class, () -> snapshot.isSupported(TestLevels.overworld(), null));
        //noinspection DataFlowIssue
        assertThrows(NullPointerException.class, () -> stone.isSupported(null));
    }

    @Test
    void everyDoorChecksTheFloorOrItsMatchingLowerHalf() {
        int checked = 0;
        for (BlockType type : BlockTypes.values()) {
            if (!type.getTraits().contains(BlockTraits.IS_DOOR_HINGE)) {
                continue;
            }

            BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
            BlockState lower = type.getDefaultState().withTrait(BlockTraits.IS_UPPER_BLOCK, false);
            BlockState upper = lower.withTrait(BlockTraits.IS_UPPER_BLOCK, true);
            assertFalse(lower.isSupported(world.block(POSITION)));
            assertFalse(upper.isSupported(world.block(POSITION)));

            world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
            assertTrue(lower.isSupported(world.block(POSITION)));
            assertFalse(upper.isSupported(world.block(POSITION)));

            world.setState(POSITION.add(0, -1, 0), lower);
            assertTrue(upper.isSupported(world.block(POSITION)));
            world.setState(POSITION.add(0, -1, 0), upper);
            assertFalse(upper.isSupported(world.block(POSITION)));
            checked++;
        }

        assertTrue(checked > 10);
    }

    @Test
    void everyButtonUsesItsCapturedAttachmentFace() {
        int checked = 0;
        for (BlockType type : BlockTypes.values()) {
            if (!type.getTraits().contains(BlockTraits.IS_BUTTON_PRESSED)) {
                continue;
            }

            for (Direction face : Direction.values()) {
                BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
                BlockState button = type.getDefaultState().withTrait(BlockTraits.FACING_DIRECTION, face);
                assertFalse(button.isSupported(world.block(POSITION)));
                world.setState(face.getOpposite().relative(POSITION), BlockStates.STONE);
                assertTrue(button.isSupported(world.block(POSITION)));
                assertSame(BlockStates.AIR, world.state(POSITION));
            }

            checked++;
        }

        assertTrue(checked > 5);
    }

    @Test
    void attachmentResolutionPrecedesPlacementValidation() {
        for (BlockType type : List.of(BlockTypes.STONE_BUTTON, BlockTypes.OAK_BUTTON, BlockTypes.LEVER)) {
            for (Direction face : Direction.values()) {
                BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
                Player player = InterfaceProxy.create(Player.class, Map.of(
                        "getLevel", world.block(POSITION).getLevel(), "getHorizontalDirection", Direction.NORTH));
                BlockState resolved = BlockTestRegistry.get().requireComponent(type, BlockComponents.RESOLVE_PLACEMENT_STATE)
                        .execute(type.getDefaultState(), world.block(POSITION), player, face, Vector3f.ZERO);
                assertNotNull(resolved);
                Block proposed = new CloudBlock(player.getLevel(), POSITION, new BlockState[]{resolved, BlockStates.AIR});
                assertFalse(proposed.requireComponent(BlockComponents.CAN_BE_PLACED).execute(proposed));

                world.setState(face.getOpposite().relative(POSITION), BlockStates.STONE);
                assertTrue(proposed.requireComponent(BlockComponents.CAN_BE_PLACED).execute(proposed));
                assertTrue(proposed.requireComponent(BlockComponents.ON_PLACE)
                        .execute(resolved, player, POSITION, face, Vector3f.ZERO));
                assertSame(resolved, world.state(POSITION));
            }
        }
    }

    @Test
    void hooksAndRailsShareTheirExistingSupportRules() {
        for (Direction face : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
            BlockState hook = BlockTypes.TRIPWIRE_HOOK.getDefaultState().withTrait(BlockTraits.DIRECTION, face);
            assertFalse(hook.isSupported(world.block(POSITION)));
            world.setState(face.getOpposite().relative(POSITION), BlockStates.STONE);
            assertTrue(hook.isSupported(world.block(POSITION)));
        }

        for (BlockType type : List.of(BlockTypes.RAIL, BlockTypes.GOLDEN_RAIL, BlockTypes.ACTIVATOR_RAIL, BlockTypes.DETECTOR_RAIL)) {
            BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
            BlockState rail = type.getDefaultState();
            assertFalse(rail.isSupported(world.block(POSITION)));
            world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
            assertTrue(rail.isSupported(world.block(POSITION)));
        }
    }

    @Test
    void ordinaryBlocksDoNotRequireASupportingFaceOrAnEmptyTarget() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockTypes.BEDROCK.getDefaultState());
        world.obstruct(POSITION);
        assertTrue(BlockStates.STONE.isSupported(world.block(POSITION)));
        assertSame(BlockTypes.BEDROCK, world.state(POSITION).getType());
        assertTrue(BlockStates.STONE.isSupported(world.block(Vector3i.from(0, -64, 0))));
        assertTrue(BlockStates.STONE.isSupported(world.block(Vector3i.from(0, 319, 0))));
    }
}
