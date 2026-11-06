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
        assertThrows(NullPointerException.class, () -> stone.isSupported((Block) null));
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
