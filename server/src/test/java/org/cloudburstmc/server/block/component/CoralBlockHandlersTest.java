package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.SeaGrassType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CoralBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void allLivingCoralsScheduleDelayedDeathAndUseTheMatchingDeadVariant() {
        for (BlockType type : BlockTypes.values()) {
            String name = type.getId().getName();
            if (!name.contains("coral") || name.startsWith("dead_")) {
                continue;
            }

            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION, type.getDefaultState());
            Block block = world.block(POSITION);

            REGISTRY.requireComponent(type, BlockComponents.ON_STATE_CHANGED).execute(block);

            assertEquals(1, world.getScheduledDelays().size(), name);
            assertTrue(world.getScheduledDelays().getFirst() >= 60 && world.getScheduledDelays().getFirst() < 100, name);

            REGISTRY.requireComponent(type, BlockComponents.ON_TICK).execute(block, null);

            BlockType expected = BlockTypes.get(Identifier.parse("dead_" + name)).orElseThrow();

            assertSame(expected, world.state(POSITION).getType(), name);
            assertEquals(1, world.getEvents(BlockFadeEvent.class).size(), name);

            REGISTRY.requireComponent(expected, BlockComponents.ON_STATE_CHANGED).execute(world.block(POSITION));

            assertEquals(1, world.getScheduledDelays().size(), name);
        }
    }

    @Test
    void waterloggingOrAnyWetNeighborPreventsDeathAndCanRescueScheduledCoral() {
        for (Direction direction : Direction.values()) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION, BlockTypes.BRAIN_CORAL.getDefaultState());
            Block block = world.block(POSITION);

            CoralBlockHandlers.ON_STATE_CHANGED.execute(block);
            world.setLiquid(direction.relative(POSITION), LiquidState.of(BlockStates.WATER));
            REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL, BlockComponents.ON_TICK).execute(block, null);

            assertSame(BlockTypes.BRAIN_CORAL.getDefaultState(), world.state(POSITION));
            assertTrue(world.getEvents(BlockFadeEvent.class).isEmpty());
        }

        BlockTestWorld waterlogged = new BlockTestWorld(REGISTRY);
        waterlogged.setState(POSITION, BlockTypes.BRAIN_CORAL.getDefaultState());
        waterlogged.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));

        CoralBlockHandlers.ON_STATE_CHANGED.execute(waterlogged.block(POSITION));

        assertTrue(waterlogged.getScheduledDelays().isEmpty());
    }

    @Test
    void wallFansUseTheirSupportingWallAndPreserveFacingWhenTheyDie() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            BlockState state = BlockTypes.BRAIN_CORAL_WALL_FAN.getDefaultState().withTrait(BlockTraits.CORAL_DIRECTION, facing);
            Vector3i support = facing.getOpposite().relative(POSITION);
            world.setState(POSITION, state);
            world.setState(support, BlockStates.STONE);
            Block block = world.block(POSITION);

            assertTrue(CoralBlockHandlers.CAN_SURVIVE.execute(block));

            REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL_WALL_FAN, BlockComponents.ON_TICK).execute(block, null);

            assertSame(BlockTypes.DEAD_BRAIN_CORAL_WALL_FAN, world.state(POSITION).getType());
            assertEquals(facing, world.state(POSITION).ensureTrait(BlockTraits.CORAL_DIRECTION));

            world.setState(support, BlockStates.AIR);
            REGISTRY.requireComponent(BlockTypes.DEAD_BRAIN_CORAL_WALL_FAN, BlockComponents.ON_NEIGHBOUR_CHANGED)
                    .execute(block, world.block(support));

            assertEquals(1, world.getBreakCount());
        }
    }

    @Test
    void livingAndDeadPlantsAndFloorFansBreakWhenTheirSupportIsRemoved() {
        for (BlockState state : List.of(BlockTypes.BRAIN_CORAL.getDefaultState(), BlockTypes.DEAD_BRAIN_CORAL.getDefaultState(),
                BlockTypes.BRAIN_CORAL_FAN.getDefaultState(), BlockTypes.DEAD_BRAIN_CORAL_FAN.getDefaultState())) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION, state);
            world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
            Block block = world.block(POSITION);

            assertTrue(CoralBlockHandlers.CAN_SURVIVE.execute(block));

            world.setState(POSITION.add(0, -1, 0), BlockStates.AIR);
            REGISTRY.requireComponent(state.getType(), BlockComponents.ON_NEIGHBOUR_CHANGED)
                    .execute(block, world.block(POSITION.add(0, -1, 0)));

            assertEquals(1, world.getBreakCount());
            assertSame(BlockStates.AIR, world.state(POSITION));
        }
    }

    @Test
    void fadeCancellationAndReplacementAreRespected() {
        BlockTestWorld canceled = new BlockTestWorld(REGISTRY);
        canceled.setState(POSITION, BlockTypes.BRAIN_CORAL.getDefaultState());
        canceled.onEvent(BlockFadeEvent.class, event -> event.setCancelled(true));

        REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL, BlockComponents.ON_TICK).execute(canceled.block(POSITION), null);

        assertSame(BlockTypes.BRAIN_CORAL.getDefaultState(), canceled.state(POSITION));

        BlockTestWorld replaced = new BlockTestWorld(REGISTRY);
        replaced.setState(POSITION, BlockTypes.BRAIN_CORAL.getDefaultState());
        replaced.onEvent(BlockFadeEvent.class, event -> event.setNewState(BlockStates.STONE));

        REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL, BlockComponents.ON_TICK).execute(replaced.block(POSITION), null);

        assertSame(BlockStates.STONE, replaced.state(POSITION));

        BlockTestWorld changedByListener = new BlockTestWorld(REGISTRY);
        changedByListener.setState(POSITION, BlockTypes.BRAIN_CORAL.getDefaultState());
        changedByListener.onEvent(BlockFadeEvent.class, event -> changedByListener.setState(POSITION, BlockStates.STONE));

        REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL, BlockComponents.ON_TICK)
                .execute(changedByListener.block(POSITION), null);

        assertSame(BlockStates.STONE, changedByListener.state(POSITION));
    }

    @Test
    void dropsRequireSilkTouchForPlantsAndFansAndPreserveTheCorrectItemForm() {
        ItemStack silkTool = ItemStack.builder(ItemTypes.DIAMOND_PICKAXE)
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1)))
                .build();
        BlockLootContext ordinary = BlockLootContext.empty(new Random(0));
        BlockLootContext silk = new BlockLootContext(silkTool, null, new Random(0));
        BlockTestWorld world = new BlockTestWorld(REGISTRY);

        for (BlockType type : List.of(BlockTypes.BRAIN_CORAL, BlockTypes.DEAD_BRAIN_CORAL,
                BlockTypes.BRAIN_CORAL_FAN, BlockTypes.DEAD_BRAIN_CORAL_FAN,
                BlockTypes.BRAIN_CORAL_WALL_FAN, BlockTypes.DEAD_BRAIN_CORAL_WALL_FAN)) {
            world.setState(POSITION, type.getDefaultState());
            Block block = world.block(POSITION);
            BlockType itemBlock = type == BlockTypes.BRAIN_CORAL_WALL_FAN ? BlockTypes.BRAIN_CORAL_FAN
                    : type == BlockTypes.DEAD_BRAIN_CORAL_WALL_FAN ? BlockTypes.DEAD_BRAIN_CORAL_FAN : type;

            assertTrue(REGISTRY.requireComponent(type, BlockComponents.GET_LOOT).execute(block, ordinary).isEmpty());

            List<ItemStack> drops = REGISTRY.requireComponent(type, BlockComponents.GET_LOOT).execute(block, silk);

            assertEquals(1, drops.size());
            assertSame(itemBlock.asItem().orElseThrow(), drops.getFirst().getType());
        }

        world.setState(POSITION, BlockStates.BRAIN_CORAL_BLOCK);
        Block coralBlock = world.block(POSITION);

        assertSame(ItemTypes.DEAD_BRAIN_CORAL_BLOCK,
                REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL_BLOCK, BlockComponents.GET_LOOT)
                        .execute(coralBlock, ordinary).getFirst().getType());
        assertSame(ItemTypes.BRAIN_CORAL_BLOCK,
                REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL_BLOCK, BlockComponents.GET_LOOT)
                        .execute(coralBlock, silk).getFirst().getType());
    }

    @Test
    void fanPlacementSelectsTheFloorOrWallVariantBeforeSurvivalChecks() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));
        Block target = world.block(POSITION);

        for (Direction face : Direction.values()) {
            BlockState state = REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL_FAN, BlockComponents.RESOLVE_PLACEMENT_STATE)
                    .execute(BlockTypes.BRAIN_CORAL_FAN.getDefaultState(), target, null, face, Vector3f.ZERO);

            if (face == Direction.DOWN) {
                assertNull(state);
                continue;
            }

            assertNotNull(state);
            assertSame(face.getAxis().isHorizontal() ? BlockTypes.BRAIN_CORAL_WALL_FAN : BlockTypes.BRAIN_CORAL_FAN, state.getType());

            if (face.getAxis().isHorizontal()) {
                assertEquals(face, state.ensureTrait(BlockTraits.CORAL_DIRECTION));
            }
        }
    }

    @Test
    void dryFansArePlacedDeadEvenBesideWaterAndDeadFansCannotBeRevived() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setLiquid(POSITION.add(1, 0, 0), LiquidState.of(BlockStates.WATER));
        Block target = world.block(POSITION);
        for (Direction face : List.of(Direction.UP, Direction.NORTH)) {
            BlockState placed = REGISTRY.requireComponent(BlockTypes.BRAIN_CORAL_FAN, BlockComponents.RESOLVE_PLACEMENT_STATE)
                    .execute(BlockTypes.BRAIN_CORAL_FAN.getDefaultState(), target, null, face, Vector3f.ZERO);

            assertNotNull(placed);
            assertSame(face == Direction.UP ? BlockTypes.DEAD_BRAIN_CORAL_FAN : BlockTypes.DEAD_BRAIN_CORAL_WALL_FAN, placed.getType());
        }

        world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));
        BlockState dead = REGISTRY.requireComponent(BlockTypes.DEAD_BRAIN_CORAL_FAN, BlockComponents.RESOLVE_PLACEMENT_STATE)
                .execute(BlockTypes.DEAD_BRAIN_CORAL_FAN.getDefaultState(), target, null, Direction.UP, Vector3f.ZERO);

        assertNotNull(dead);
        assertSame(BlockTypes.DEAD_BRAIN_CORAL_FAN, dead.getType());
    }

    @Test
    void underwaterBoneMealProposesSupportedPlantsWithoutChangingTheWorld() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                world.setState(POSITION.add(x, -1, z), BlockStates.SAND);
                world.setState(POSITION.add(x, 0, z), BlockStates.WATER);
                world.setState(POSITION.add(x, 1, z), BlockStates.WATER);
            }
        }

        FertilizationResult result = UnderwaterPlantBlockHandlers.spread(world.block(POSITION), Direction.UP, new Random(1));

        assertTrue(result.succeeded());
        assertEquals(result.changes().size(), result.changes().stream().map(change -> change.block().getPosition()).distinct().count());

        for (BlockChange change : result.changes()) {
            assertTrue(change.newState().is(BlockTags.UNDERWATER_BONEMEALS));
            assertSame(BlockStates.WATER, world.state(change.block().getPosition()));
        }
    }

    @Test
    void underwaterBoneMealCanSelectLivingCoralFromTheGrowthTag() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION.add(0, -1, 0), BlockStates.SAND);
        world.setState(POSITION, BlockStates.WATER);

        List<BlockType> candidates = REGISTRY.getTag(BlockTags.UNDERWATER_BONEMEALS).getValues().stream()
                .sorted(Comparator.comparing(type -> type.getId().toString()))
                .toList();
        int coralIndex = candidates.indexOf(BlockTypes.BRAIN_CORAL);
        assertTrue(coralIndex >= 0);

        Random random = new Random(0) {
            @Override
            public int nextInt(int bound) {
                return bound == candidates.size() ? coralIndex : 0;
            }
        };

        FertilizationResult result = UnderwaterPlantBlockHandlers.spread(world.block(POSITION), Direction.UP, random);
        assertTrue(result.succeeded());
        assertEquals(1, result.changes().size());
        assertSame(BlockTypes.BRAIN_CORAL.getDefaultState(), result.changes().getFirst().newState());
        assertSame(BlockStates.WATER, world.state(POSITION));
    }

    @Test
    void underwaterBoneMealRejectsFlowingWaterAndTallSeagrassNeedsSpace() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1));

        assertFalse(UnderwaterPlantBlockHandlers.spread(world.block(POSITION), Direction.UP, new Random(0)).succeeded());

        world.setState(POSITION, BlockTypes.SEAGRASS.getDefaultState());
        world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));
        world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);

        assertFalse(UnderwaterPlantBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(0)).succeeded());

        world.setState(POSITION.add(0, 1, 0), BlockStates.WATER);
        FertilizationResult result = UnderwaterPlantBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(0));

        assertEquals(2, result.changes().size());
        assertEquals(SeaGrassType.DOUBLE_BOTTOM, result.changes().getFirst().newState().ensureTrait(BlockTraits.SEA_GRASS_TYPE));
        assertEquals(SeaGrassType.DOUBLE_TOP, result.changes().getLast().newState().ensureTrait(BlockTraits.SEA_GRASS_TYPE));
    }

    @Test
    void breakingEitherHalfOfTallSeagrassRemovesBothWithoutDroppingTwice() {
        for (SeaGrassType half : List.of(SeaGrassType.DOUBLE_BOTTOM, SeaGrassType.DOUBLE_TOP)) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            Vector3i top = POSITION.add(0, 1, 0);
            world.setState(POSITION, BlockTypes.SEAGRASS.getDefaultState().withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_BOTTOM));
            world.setState(top, BlockTypes.SEAGRASS.getDefaultState().withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_TOP));

            Block broken = world.block(half == SeaGrassType.DOUBLE_TOP ? top : POSITION);
            Vector3i partnerPosition = half == SeaGrassType.DOUBLE_TOP ? POSITION : top;
            BlockState partnerState = world.state(partnerPosition);
            BlockLootContext shears = new BlockLootContext(ItemStack.from(ItemTypes.SHEARS), null, new Random(0));

            List<ItemStack> drops = UnderwaterPlantBlockHandlers.GET_LOOT.execute(broken, shears);
            UnderwaterPlantBlockHandlers.ON_DESTROY.execute(broken, null);

            assertEquals(1, drops.size());
            assertEquals(2, drops.getFirst().getCount());
            assertSame(BlockStates.AIR, world.state(POSITION));
            assertSame(BlockStates.AIR, world.state(top));
            assertEquals(0, world.getBreakCount());
            assertEquals(List.of(new BlockTestWorld.BlockBreakEffect(partnerPosition, partnerState)), world.getBlockBreakEffects());
            assertEquals(List.of(broken.getPosition(), partnerPosition), world.getNeighborUpdates());
        }
    }

    @Test
    void breakingTallSeagrassDoesNotDestroyAnUnrelatedNeighbor() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        Vector3i top = POSITION.add(0, 1, 0);
        world.setState(POSITION, BlockTypes.SEAGRASS.getDefaultState().withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_BOTTOM));
        world.setState(top, BlockTypes.STONE.getDefaultState());

        UnderwaterPlantBlockHandlers.ON_DESTROY.execute(world.block(POSITION), null);

        assertSame(BlockStates.AIR, world.state(POSITION));
        assertSame(BlockTypes.STONE.getDefaultState(), world.state(top));
        assertEquals(List.of(), world.getBlockBreakEffects());
    }

    @Test
    void doublePlantsUseTheSamePartnerBreakEffect() {
        for (boolean upper : List.of(false, true)) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            Vector3i top = POSITION.add(0, 1, 0);
            world.setState(POSITION, BlockTypes.LILAC.getDefaultState().withTrait(BlockTraits.IS_UPPER_BLOCK, false));
            world.setState(top, BlockTypes.LILAC.getDefaultState().withTrait(BlockTraits.IS_UPPER_BLOCK, true));

            Vector3i brokenPosition = upper ? top : POSITION;
            Vector3i partnerPosition = upper ? POSITION : top;
            BlockState partnerState = world.state(partnerPosition);

            VegetationBlockHandlers.DOUBLE_PLANT_DESTROY.execute(world.block(brokenPosition), null);

            assertSame(BlockStates.AIR, world.state(POSITION));
            assertSame(BlockStates.AIR, world.state(top));
            assertEquals(List.of(new BlockTestWorld.BlockBreakEffect(partnerPosition, partnerState)), world.getBlockBreakEffects());
            assertEquals(List.of(brokenPosition, partnerPosition), world.getNeighborUpdates());
            assertEquals(0, world.getBreakCount());
        }
    }
}
