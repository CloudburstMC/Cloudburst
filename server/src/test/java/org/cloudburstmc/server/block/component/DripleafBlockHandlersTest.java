package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.entity.EntityInsideBlockEvent;
import org.cloudburstmc.api.event.player.PlayerInteractEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.api.util.data.DripleafTilt;
import org.cloudburstmc.api.util.data.LeverDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.entity.EntityInsideBlockScanner;
import org.cloudburstmc.server.item.component.BoneMealItemHandlers;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DripleafBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final BlockState HEAD = BlockTypes.BIG_DRIPLEAF.getDefaultState().withTrait(BlockTraits.DRIPLEAF_HEAD, true)
            .withTrait(BlockTraits.DRIPLEAF_TILT, DripleafTilt.NONE).withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.EAST);
    private static final BlockState SMALL = BlockTypes.SMALL_DRIPLEAF_BLOCK.getDefaultState()
            .withTrait(BlockTraits.IS_UPPER_BLOCK, false).withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.EAST);

    @Test
    void smallPlacementRequiresRoomAndPreservesWaterInBothHalves() {
        BlockTestWorld world = world();
        world.setState(POSITION, BlockStates.WATER);
        world.setState(POSITION.add(0, 1, 0), BlockStates.WATER);
        Player player = InterfaceProxy.create(Player.class, Map.of("getLevel", world.block(POSITION).getLevel(),
                "getHorizontalDirection", Direction.WEST));
        BlockState state = SmallDripleafBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(SMALL, world.block(POSITION), player, Direction.UP, Vector3f.ZERO);

        assertNotNull(state);
        assertTrue(SmallDripleafBlockHandlers.ON_PLACE.execute(state, player, POSITION, Direction.UP, Vector3f.ZERO));
        assertFalse(world.state(POSITION).ensureTrait(BlockTraits.IS_UPPER_BLOCK));
        assertTrue(world.state(POSITION.add(0, 1, 0)).ensureTrait(BlockTraits.IS_UPPER_BLOCK));
        assertSame(BlockStates.WATER, world.block(POSITION).getSecondaryState());
        assertSame(BlockStates.WATER, world.block(POSITION.add(0, 1, 0)).getSecondaryState());

        world.setState(POSITION, BlockStates.AIR);
        world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
        assertNull(SmallDripleafBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(SMALL, world.block(POSITION), player, Direction.UP, Vector3f.ZERO));
        assertFalse(SmallDripleafBlockHandlers.ON_PLACE.execute(SMALL, player, POSITION, Direction.UP, Vector3f.ZERO));
        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void smallPlacementPreservesSourcesAndFallingWaterButDisplacesShallowFlow() {
        BlockState flowing = BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1);
        BlockState falling = BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 8);
        List<BlockState> contents = List.of(BlockStates.AIR, BlockStates.WATER, flowing, falling);

        for (BlockState lower : contents) {
            for (BlockState upper : contents) {
                BlockTestWorld world = world();
                world.setState(POSITION, lower);
                world.setState(POSITION.add(0, 1, 0), upper);
                Player player = InterfaceProxy.create(Player.class, Map.of("getLevel", world.block(POSITION).getLevel()));

                assertTrue(SmallDripleafBlockHandlers.ON_PLACE.execute(SMALL, player, POSITION, Direction.UP, Vector3f.ZERO));

                assertSame(lower == flowing ? BlockStates.AIR : lower, world.block(POSITION).getSecondaryState());
                assertSame(upper == flowing ? BlockStates.AIR : upper, world.block(POSITION.add(0, 1, 0)).getSecondaryState());
            }
        }
    }

    @Test
    void smallDripleafRequiresWaterForPlantingAndSurvivalOnDirt() {
        BlockTestWorld world = world();
        world.setState(POSITION, SMALL);
        assertTrue(SmallDripleafBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

        world.setState(POSITION.add(0, -1, 0), BlockStates.DIRT);
        assertFalse(SmallDripleafBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        world.setState(POSITION, BlockStates.AIR);
        assertNull(SmallDripleafBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(SMALL, world.block(POSITION), null, Direction.UP, Vector3f.ZERO));
        world.setState(POSITION, SMALL);
        world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));
        assertTrue(SmallDripleafBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        assertFalse(SmallDripleafBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
    }

    @Test
    void removingWaterSchedulesDestructionOnDirtButNotOnClay() {
        for (BlockState soil : List.of(BlockStates.DIRT, BlockStates.CLAY)) {
            BlockTestWorld world = world();
            world.setState(POSITION.add(0, -1, 0), soil);
            world.setState(POSITION, SMALL);
            world.setState(POSITION.add(0, 1, 0), SMALL.withTrait(BlockTraits.IS_UPPER_BLOCK, true));
            world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));
            SmallDripleafBlockHandlers.ON_STATE_CHANGED.execute(world.block(POSITION));
            assertSame(SMALL, world.state(POSITION));

            world.setLiquid(POSITION, LiquidState.empty());
            SmallDripleafBlockHandlers.ON_STATE_CHANGED.execute(world.block(POSITION));

            assertEquals(0, world.getBreakCount());
            assertSame(SMALL, world.state(POSITION));
            assertEquals(soil == BlockStates.DIRT ? Map.of(POSITION, 1) : Map.of(), world.getScheduledUpdates());

            SmallDripleafBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));
            assertEquals(soil == BlockStates.DIRT ? 1 : 0, world.getBreakCount());
        }
    }

    @Test
    void everyWaterDepthAllowsPlantingButOnlySourcesAndFallingWaterKeepThePlantAlive() {
        for (int depth = 0; depth < 16; depth++) {
            BlockTestWorld world = world();
            world.setState(POSITION.add(0, -1, 0), BlockStates.DIRT);
            BlockState water = BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, depth);
            world.setState(POSITION, water);
            Player player = InterfaceProxy.create(Player.class, Map.of("getLevel", world.block(POSITION).getLevel(),
                    "getHorizontalDirection", Direction.WEST));

            BlockState state = SmallDripleafBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(SMALL, world.block(POSITION), player, Direction.UP, Vector3f.ZERO);
            assertNotNull(state);
            BlockLayers layers = BlockLayerRules.resolveReplacement(new BlockLayers(water, BlockStates.AIR), BlockLayer.PRIMARY, state);
            assertNotNull(layers);
            Block proposed = InterfaceProxy.create(Block.class, Map.of("getLevel", world.block(POSITION).getLevel(),
                    "getPosition", POSITION, "getState", layers.primary(), "getLiquid", LiquidState.of(layers.secondary())));
            boolean survives = depth == 0 || depth >= 8;
            assertTrue(REGISTRY.requireComponent(BlockTypes.SMALL_DRIPLEAF_BLOCK, BlockComponents.CAN_BE_PLACED).execute(proposed));
            assertEquals(survives, SmallDripleafBlockHandlers.CAN_SURVIVE.execute(proposed));

            assertTrue(SmallDripleafBlockHandlers.ON_PLACE.execute(state, player, POSITION, Direction.UP, Vector3f.ZERO));
            SmallDripleafBlockHandlers.ON_STATE_CHANGED.execute(world.block(POSITION));
            SmallDripleafBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, 1, 0)));

            assertEquals(0, world.getBreakCount());
            assertSame(BlockTypes.SMALL_DRIPLEAF_BLOCK, world.state(POSITION).getType());
            assertSame(depth == 0 || depth >= 8 ? water : BlockStates.AIR, world.block(POSITION).getSecondaryState());
            assertEquals(survives ? Map.of() : Map.of(POSITION, 1), world.getScheduledUpdates());

            SmallDripleafBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));
            assertEquals(survives ? 0 : 1, world.getBreakCount());
            assertSame(survives ? state : BlockStates.AIR, world.state(POSITION));

            BlockTestWorld unsupported = world();
            unsupported.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
            unsupported.setState(POSITION, water);
            assertNull(SmallDripleafBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(SMALL, unsupported.block(POSITION), null, Direction.UP, Vector3f.ZERO));
        }
    }

    @Test
    void stemStateChangesReplaceTiltTicksWithSupportChecksOnlyWhenNeeded() {
        BlockTestWorld world = world();
        world.setState(POSITION, HEAD.withTrait(BlockTraits.DRIPLEAF_TILT, DripleafTilt.FULL_TILT));
        world.block(POSITION).getLevel().scheduleUpdate(POSITION, 100);
        world.setState(POSITION, BigDripleafBlockHandlers.stem(HEAD));
        world.setState(POSITION.add(0, 1, 0), HEAD);

        BigDripleafBlockHandlers.ON_STATE_CHANGED.execute(world.block(POSITION));
        assertTrue(world.getScheduledUpdates().isEmpty());

        world.setState(POSITION.add(0, 1, 0), BlockStates.AIR);
        world.block(POSITION).getLevel().scheduleUpdate(POSITION, 1);
        BigDripleafBlockHandlers.ON_STATE_CHANGED.execute(world.block(POSITION));
        assertEquals(Map.of(POSITION, 1), world.getScheduledUpdates());
    }

    @Test
    void fertilizingEitherSmallHalfCreatesTheSameFacingBigDripleafColumn() {
        for (boolean upper : List.of(false, true)) {
            BlockTestWorld world = world();
            world.setState(POSITION, SMALL);
            world.setState(POSITION.add(0, 1, 0), SMALL.withTrait(BlockTraits.IS_UPPER_BLOCK, true));
            Block origin = world.block(upper ? POSITION.add(0, 1, 0) : POSITION);
            FertilizationResult result = SmallDripleafBlockHandlers.FERTILIZE.execute(origin, null, new Random(1));

            assertTrue(result.changes().size() >= 2 && result.changes().size() <= 5);
            assertTrue(BoneMealItemHandlers.applyFertilization(origin, null, result));
            for (int y = 0; y < result.changes().size(); y++) {
                BlockState state = world.state(POSITION.add(0, y, 0));
                assertSame(BlockTypes.BIG_DRIPLEAF, state.getType());
                assertEquals(CardinalDirection.EAST, state.ensureTrait(BlockTraits.CARDINAL_DIRECTION));
                assertEquals(y == result.changes().size() - 1, state.ensureTrait(BlockTraits.DRIPLEAF_HEAD));
                assertEquals(DripleafTilt.NONE, state.ensureTrait(BlockTraits.DRIPLEAF_TILT));
            }
        }
    }

    @Test
    void fertilizingAnyBigStemExtendsItsHeadButNotThroughAnObstruction() {
        BlockTestWorld world = world();
        world.setState(POSITION, BigDripleafBlockHandlers.stem(HEAD));
        world.setState(POSITION.add(0, 1, 0), HEAD);
        FertilizationResult result = BigDripleafBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(0));

        assertEquals(2, result.changes().size());
        assertTrue(BoneMealItemHandlers.applyFertilization(world.block(POSITION), null, result));
        assertFalse(world.state(POSITION.add(0, 1, 0)).ensureTrait(BlockTraits.DRIPLEAF_HEAD));
        assertTrue(world.state(POSITION.add(0, 2, 0)).ensureTrait(BlockTraits.DRIPLEAF_HEAD));

        world.setState(POSITION.add(0, 3, 0), BlockStates.STONE);
        assertFalse(BigDripleafBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(0)).succeeded());
    }

    @Test
    void standingEntitiesTriggerTheTimedTiltCycleEvenWhileSneaking() {
        BlockTestWorld world = world();
        world.setState(POSITION, HEAD);
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), standingPlayer(), true);
        assertEquals(DripleafTilt.UNSTABLE, world.state(POSITION).ensureTrait(BlockTraits.DRIPLEAF_TILT));

        for (DripleafTilt tilt : List.of(DripleafTilt.PARTIAL_TILT, DripleafTilt.FULL_TILT, DripleafTilt.NONE)) {
            BigDripleafBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));
            assertEquals(tilt, world.state(POSITION).ensureTrait(BlockTraits.DRIPLEAF_TILT));
        }

        assertEquals(List.of(10, 10, 100), world.getScheduledDelays());
        assertEquals(List.of(SoundTypes.TILT_DOWN_BIG_DRIPLEAF, SoundTypes.TILT_DOWN_BIG_DRIPLEAF, SoundTypes.TILT_UP_BIG_DRIPLEAF), world.getSounds());
        assertEquals(1, world.getEvents(EntityInsideBlockEvent.class).size());
        assertEquals(1, world.getEvents(PlayerInteractEvent.class).size());
        assertEquals(1, world.getEvents(EntityChangeBlockEvent.class).size());
    }

    @Test
    void cancelledInteractionsAndEntityChangesDoNotTiltOrSchedule() {
        for (Class<? extends Event> type : List.of(
                EntityInsideBlockEvent.class, PlayerInteractEvent.class, EntityChangeBlockEvent.class)) {
            BlockTestWorld world = world();
            world.setState(POSITION, HEAD);
            world.onEvent(type, event -> event.setCancelled(true));

            EntityInsideBlockScanner.applyEffects(world.block(POSITION), standingPlayer(), true);
            assertSame(HEAD, world.state(POSITION));
            assertTrue(world.getScheduledDelays().isEmpty());
            assertTrue(world.getSounds().isEmpty());
        }
    }

    @Test
    void projectilesTiltImmediatelyAndPowerRestoresTheLeaf() {
        BlockTestWorld world = world();
        world.setState(POSITION, HEAD);
        Entity projectile = InterfaceProxy.create(Entity.class, Map.of("getType", EntityTypes.ARROW));
        BigDripleafBlockHandlers.ON_PROJECTILE_HIT.execute(world.block(POSITION), projectile);

        assertEquals(DripleafTilt.FULL_TILT, world.state(POSITION).ensureTrait(BlockTraits.DRIPLEAF_TILT));
        assertEquals(List.of(100), world.getScheduledDelays());

        Vector3i power = POSITION.add(1, 0, 0);
        world.setState(power, BlockTypes.REDSTONE_BLOCK.getDefaultState());
        BigDripleafBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(power));
        assertEquals(DripleafTilt.NONE, world.state(POSITION).ensureTrait(BlockTraits.DRIPLEAF_TILT));

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), standingPlayer(), true);
        assertEquals(DripleafTilt.NONE, world.state(POSITION).ensureTrait(BlockTraits.DRIPLEAF_TILT));
    }

    @Test
    void cancellingProjectileChangesPreservesTheSupportingLeaf() {
        BlockTestWorld world = world();
        world.setState(POSITION, HEAD);
        world.onEvent(EntityChangeBlockEvent.class, event -> event.setCancelled(true));
        Entity projectile = InterfaceProxy.create(Entity.class, Map.of("getType", EntityTypes.ARROW));

        BigDripleafBlockHandlers.ON_PROJECTILE_HIT.execute(world.block(POSITION), projectile);

        assertSame(HEAD, world.state(POSITION));
        assertTrue(world.getScheduledDelays().isEmpty());
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void tiltedLeafAndStemUseTheirStateSpecificCollisionShapes() {
        assertEquals(15f / 16, HEAD.getCollisionShape().bounds().getMaxY());
        assertEquals(13f / 16, HEAD.withTrait(BlockTraits.DRIPLEAF_TILT, DripleafTilt.PARTIAL_TILT)
                .getCollisionShape().bounds().getMaxY());
        assertTrue(HEAD.withTrait(BlockTraits.DRIPLEAF_TILT, DripleafTilt.FULL_TILT).getCollisionShape().isEmpty());
        assertTrue(BigDripleafBlockHandlers.stem(HEAD).getCollisionShape().isEmpty());
    }

    @Test
    void stackedLeavesBecomeStemsAndUnsupportedStemsBreakOnTheirScheduledTick() {
        BlockTestWorld world = world();
        world.setState(POSITION, HEAD);
        world.setState(POSITION.add(0, 1, 0), HEAD);

        BigDripleafBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, 1, 0)));
        assertFalse(world.state(POSITION).ensureTrait(BlockTraits.DRIPLEAF_HEAD));

        world.setState(POSITION.add(0, -1, 0), BlockStates.AIR);
        BigDripleafBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, -1, 0)));
        assertEquals(List.of(1), world.getScheduledDelays());
        assertSame(BlockTypes.BIG_DRIPLEAF, world.state(POSITION).getType());

        BigDripleafBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));
        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void aLeverCanLockTheLeafThroughItsSupportingConductor() {
        BlockTestWorld world = world();
        world.setState(POSITION, HEAD);
        world.setState(POSITION.add(1, 0, 0), BlockStates.STONE);
        Vector3i lever = POSITION.add(2, 0, 0);
        BlockState on = BlockTypes.LEVER.getDefaultState().withTrait(BlockTraits.IS_OPEN, true)
                .withTrait(BlockTraits.LEVER_DIRECTION, LeverDirection.EAST);
        world.setState(lever, on);

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), standingPlayer(), true);
        assertSame(HEAD, world.state(POSITION));

        world.setState(lever, on.withTrait(BlockTraits.IS_OPEN, false));
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), standingPlayer(), true);
        assertEquals(DripleafTilt.UNSTABLE, world.state(POSITION).ensureTrait(BlockTraits.DRIPLEAF_TILT));
    }

    @Test
    void smallDripleafRequiresShearsAndRemovingOneHalfRemovesItsPartner() {
        BlockTestWorld world = world();
        world.setState(POSITION, SMALL);
        world.setState(POSITION.add(0, 1, 0), SMALL.withTrait(BlockTraits.IS_UPPER_BLOCK, true));
        BlockLootContext bare = new BlockLootContext(ItemStack.EMPTY, null, new Random(0));
        BlockLootContext shears = new BlockLootContext(ItemStack.from(ItemTypes.SHEARS), null, new Random(0));

        assertTrue(REGISTRY.requireComponent(BlockTypes.SMALL_DRIPLEAF_BLOCK, BlockComponents.GET_LOOT).execute(world.block(POSITION), bare).isEmpty());
        assertEquals(1, REGISTRY.requireComponent(BlockTypes.SMALL_DRIPLEAF_BLOCK, BlockComponents.GET_LOOT).execute(world.block(POSITION), shears).size());
        SmallDripleafBlockHandlers.ON_DESTROY.execute(world.block(POSITION), null);
        assertSame(BlockStates.AIR, world.state(POSITION));
        assertSame(BlockStates.AIR, world.state(POSITION.add(0, 1, 0)));
        assertEquals(1, world.getBlockBreakEffects().size());
    }

    private static Player standingPlayer() {
        return InterfaceProxy.create(Player.class, Map.of("isOnGround", true, "isSneaking", true,
                "getPosition", POSITION.toFloat().add(0.5f, 15f / 16, 0.5f)));
    }

    private static BlockTestWorld world() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION.add(0, -1, 0), BlockStates.CLAY);
        return world;
    }
}
