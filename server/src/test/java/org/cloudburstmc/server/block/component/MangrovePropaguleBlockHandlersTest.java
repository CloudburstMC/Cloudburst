package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class MangrovePropaguleBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final BlockState HANGING = BlockStates.MANGROVE_PROPAGULE
            .withTrait(BlockTraits.IS_HANGING, true).withTrait(BlockTraits.PROPAGULE_STAGE, 0);
    private static final RandomGenerator RANDOM = () -> 0L;

    @Test
    void placementResetsHangingAgeAndPreservesSourceWater() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION.add(0, -1, 0), BlockStates.CLAY);
        BlockState planted = MangrovePropaguleBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                HANGING, world.block(POSITION), null, Direction.UP, Vector3f.ZERO);

        assertNotNull(planted);
        assertFalse(planted.ensureTrait(BlockTraits.IS_HANGING));
        assertEquals(4, planted.ensureTrait(BlockTraits.PROPAGULE_STAGE));

        BlockLayers layers = BlockLayerRules.resolveReplacement(new BlockLayers(BlockStates.WATER, BlockStates.AIR),
                BlockLayer.PRIMARY, planted);

        assertNotNull(layers);
        assertSame(planted, layers.primary());
        assertSame(BlockStates.WATER, layers.secondary());

        world.setState(POSITION, planted);
        assertTrue(MangrovePropaguleBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        assertFalse(MangrovePropaguleBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

        MangrovePropaguleBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, -1, 0)));
        assertSame(BlockStates.AIR, world.state(POSITION));
        assertEquals(1, world.getBreakCount());
    }

    @Test
    void hangingGrowthStopsAtMaturityAndHonorsEvents() {
        BlockTestWorld world = hangingWorld();
        assertTrue(REGISTRY.requireComponent(BlockTypes.MANGROVE_PROPAGULE, BlockComponents.CAN_RANDOM_TICK));

        for (int age = 1; age <= 4; age++) {
            tick(world);
            assertEquals(age, world.state(POSITION).ensureTrait(BlockTraits.PROPAGULE_STAGE));
        }

        tick(world);
        assertEquals(4, world.getEvents(BlockGrowEvent.class).size());

        world.setState(POSITION, HANGING);
        world.onEvent(BlockGrowEvent.class, event -> event.setCancelled(true));
        tick(world);
        assertSame(HANGING, world.state(POSITION));

        world.onEvent(BlockGrowEvent.class, event -> event.setNewState(BlockStates.STONE));
        tick(world);
        assertSame(BlockStates.STONE, world.state(POSITION));

        world.setState(POSITION, HANGING);
        world.onEvent(BlockGrowEvent.class, event -> world.setState(POSITION, BlockStates.AIR));
        tick(world);
        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void boneMealCreatesAndMaturesHangingPropagulesWithoutApplyingChangesEarly() {
        BlockTestWorld world = hangingWorld();
        world.setState(POSITION, BlockStates.AIR);
        Block leaves = world.block(POSITION.add(0, 1, 0));
        FertilizationResult creation = REGISTRY.requireComponent(BlockTypes.MANGROVE_LEAVES, BlockComponents.FERTILIZE)
                .execute(leaves, null, RANDOM);

        assertEquals(1, creation.changes().size());
        assertEquals(POSITION, creation.changes().getFirst().block().getPosition());
        assertSame(HANGING, creation.changes().getFirst().newState());
        assertSame(BlockStates.AIR, world.state(POSITION));

        world.setState(POSITION, HANGING);
        assertFalse(MangrovePropaguleBlockHandlers.FERTILIZE_LEAVES.execute(leaves, null, RANDOM).succeeded());

        for (int age = 1; age <= 4; age++) {
            FertilizationResult growth = MangrovePropaguleBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, RANDOM);
            assertEquals(age - 1, world.state(POSITION).ensureTrait(BlockTraits.PROPAGULE_STAGE));
            assertEquals(1, growth.changes().size());

            BlockState next = growth.changes().getFirst().newState();
            assertEquals(age, next.ensureTrait(BlockTraits.PROPAGULE_STAGE));
            world.setState(POSITION, next);
        }

        assertFalse(MangrovePropaguleBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, RANDOM).succeeded());

        world.setState(POSITION, BlockStates.WATER);
        assertFalse(MangrovePropaguleBlockHandlers.FERTILIZE_LEAVES.execute(leaves, null, RANDOM).succeeded());

        world.setState(POSITION, BlockStates.AIR);
        world.unload(POSITION);
        assertFalse(MangrovePropaguleBlockHandlers.FERTILIZE_LEAVES.execute(leaves, null, RANDOM).succeeded());
    }

    @Test
    void hangingSupportMustBeMangroveLeavesAndOnlyMaturePropagulesDrop() {
        BlockTestWorld world = hangingWorld();
        BlockLootContext context = new BlockLootContext(ItemStack.EMPTY, null, RANDOM);

        for (int age = 0; age <= 4; age++) {
            world.setState(POSITION, HANGING.withTrait(BlockTraits.PROPAGULE_STAGE, age));
            List<ItemStack> drops = MangrovePropaguleBlockHandlers.GET_LOOT.execute(world.block(POSITION), context);
            assertEquals(age == 4 ? 1 : 0, drops.size());
            if (!drops.isEmpty()) {
                assertSame(ItemTypes.MANGROVE_PROPAGULE, drops.getFirst().getType());
            }
        }

        world.setState(POSITION.add(0, 1, 0), BlockStates.OAK_LEAVES);
        assertFalse(MangrovePropaguleBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

        MangrovePropaguleBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, 1, 0)));
        assertSame(BlockStates.AIR, world.state(POSITION));
        assertEquals(1, world.getBreakCount());
    }

    private static void tick(BlockTestWorld world) {
        REGISTRY.requireComponent(BlockTypes.MANGROVE_PROPAGULE, BlockComponents.ON_RANDOM_TICK).execute(world.block(POSITION), RANDOM);
    }

    private static BlockTestWorld hangingWorld() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, HANGING);
        world.setState(POSITION.add(0, 1, 0), BlockStates.MANGROVE_LEAVES);
        return world;
    }
}
