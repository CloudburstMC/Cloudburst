package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.SpongeAbsorbEvent;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpongeBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void absorbsOnlyConnectedWaterAndLeavesLavaAlone() {
        BlockTestWorld world = spongeWorld();
        Vector3i water = POSITION.add(1, 0, 0);
        Vector3i lava = POSITION.add(-1, 0, 0);
        Vector3i isolated = POSITION.add(3, 0, 0);
        world.setState(water, BlockStates.WATER);
        world.setState(lava, BlockStates.LAVA);
        world.setState(isolated, BlockStates.WATER);

        SpongeBlockHandlers.absorb(world.block(POSITION));

        assertSame(BlockStates.AIR, world.state(water));
        assertSame(BlockStates.LAVA, world.state(lava));
        assertSame(BlockStates.WATER, world.state(isolated));
        assertSame(BlockTypes.WET_SPONGE, world.state(POSITION).getType());
        assertEquals(1, world.getEvents(SpongeAbsorbEvent.class).size());
    }

    @Test
    void limitsAbsorptionToSixtyFourBlocksAndSixSteps() {
        BlockTestWorld line = spongeWorld();
        for (int x = 1; x <= 7; x++) {
            line.setState(POSITION.add(x, 0, 0), BlockStates.WATER);
        }

        SpongeBlockHandlers.absorb(line.block(POSITION));

        for (int x = 1; x <= 6; x++) {
            assertSame(BlockStates.AIR, line.state(POSITION.add(x, 0, 0)));
        }
        assertSame(BlockStates.WATER, line.state(POSITION.add(7, 0, 0)));

        BlockTestWorld pool = spongeWorld();
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        pool.setState(POSITION.add(x, y, z), BlockStates.WATER);
                    }
                }
            }
        }

        SpongeBlockHandlers.absorb(pool.block(POSITION));

        assertEquals(64, pool.getEvents(SpongeAbsorbEvent.class).getFirst().getBlocks().size());
        assertEquals(65, pool.getNeighborUpdates().size());
    }

    @Test
    void eventCancellationEditsAndStaleStatesAreRespected() {
        Vector3i water = POSITION.add(1, 0, 0);
        BlockTestWorld cancelled = spongeWorld();
        cancelled.setState(water, BlockStates.WATER);
        cancelled.onEvent(SpongeAbsorbEvent.class, event -> {
            assertSame(BlockStates.WATER, cancelled.state(water));
            assertSame(BlockStates.AIR, event.getBlocks().getFirst().primaryState());
            event.setCancelled(true);
        });

        SpongeBlockHandlers.absorb(cancelled.block(POSITION));

        assertSame(BlockStates.WATER, cancelled.state(water));
        assertSame(BlockTypes.SPONGE, cancelled.state(POSITION).getType());

        BlockTestWorld edited = spongeWorld();
        edited.setState(water, BlockStates.WATER);
        edited.onEvent(SpongeAbsorbEvent.class, event -> event.getBlocks().set(0,
                new BlockReplacement(event.getBlocks().getFirst().block(), BlockStates.STONE, BlockStates.AIR)));

        SpongeBlockHandlers.absorb(edited.block(POSITION));

        assertSame(BlockStates.STONE, edited.state(water));
        assertSame(BlockTypes.WET_SPONGE, edited.state(POSITION).getType());

        BlockTestWorld stale = spongeWorld();
        stale.setState(water, BlockStates.WATER);
        stale.onEvent(SpongeAbsorbEvent.class, event -> stale.setState(water, BlockStates.STONE));

        SpongeBlockHandlers.absorb(stale.block(POSITION));

        assertSame(BlockStates.STONE, stale.state(water));
        assertSame(BlockTypes.SPONGE, stale.state(POSITION).getType());
    }

    @Test
    void drainsWaterloggedBlocksAndRemovesPlantsWithoutBreakEffects() {
        BlockTestWorld world = spongeWorld();
        Vector3i slab = POSITION.add(1, 0, 0);
        Vector3i kelp = POSITION.add(-1, 0, 0);
        Vector3i bubble = POSITION.add(0, 0, 1);
        BlockState slabState = BlockTypes.OAK_SLAB.getDefaultState();
        world.setState(slab, slabState);
        world.setLiquid(slab, LiquidState.of(BlockStates.WATER));
        world.setState(kelp, BlockTypes.KELP.getDefaultState());
        world.setLiquid(kelp, LiquidState.of(BlockStates.WATER));
        world.setState(bubble, BlockTypes.BUBBLE_COLUMN.getDefaultState());
        world.setLiquid(bubble, LiquidState.of(BlockStates.WATER));

        SpongeBlockHandlers.absorb(world.block(POSITION));

        assertSame(slabState, world.state(slab));
        assertTrue(world.block(slab).getLiquid().isEmpty());
        assertSame(BlockStates.AIR, world.state(kelp));
        assertTrue(world.block(kelp).getLiquid().isEmpty());
        assertSame(BlockStates.AIR, world.state(bubble));
        assertTrue(world.block(bubble).getLiquid().isEmpty());
        assertEquals(1, world.getDrops().size());
        assertSame(ItemTypes.KELP, world.getDrops().getFirst().getType());
        assertEquals(0, world.getBreakCount());
        assertTrue(world.getBlockBreakEffects().isEmpty());
    }

    @Test
    void removedEntriesAndUnloadedWaterAreNotAbsorbed() {
        BlockTestWorld world = spongeWorld();
        Vector3i water = POSITION.add(1, 0, 0);
        Vector3i unloaded = POSITION.add(-1, 0, 0);
        world.setState(water, BlockStates.WATER);
        world.setState(unloaded, BlockStates.WATER);
        world.unload(unloaded);
        world.onEvent(SpongeAbsorbEvent.class, event -> event.getBlocks().clear());

        SpongeBlockHandlers.absorb(world.block(POSITION));

        assertSame(BlockStates.WATER, world.state(water));
        assertSame(BlockStates.WATER, world.state(unloaded));
        assertSame(BlockTypes.SPONGE, world.state(POSITION).getType());
        assertTrue(world.getNeighborUpdates().isEmpty());
    }

    @Test
    void wetSpongesDryOnlyWhereWaterEvaporates() {
        for (boolean waterEvaporates : List.of(false, true)) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION, BlockTypes.WET_SPONGE.getDefaultState());

            SpongeBlockHandlers.dry(world.block(POSITION), waterEvaporates);
            assertSame(waterEvaporates ? BlockTypes.SPONGE : BlockTypes.WET_SPONGE, world.state(POSITION).getType());
        }
    }

    private static BlockTestWorld spongeWorld() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockTypes.SPONGE.getDefaultState());
        return world;
    }
}
