package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConduitStructureTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i ORIGIN = Vector3i.from(0, 64, 0);

    @Test
    void countsOnlyTheThreeFrameRingsAndAcceptsAllFrameMaterials() {
        BlockTestWorld world = waterWorld();
        List<Vector3i> positions = framePositions();
        List<BlockType> materials = List.of(BlockTypes.PRISMARINE, BlockTypes.PRISMARINE_BRICKS, BlockTypes.DARK_PRISMARINE, BlockTypes.SEA_LANTERN);
        for (int i = 0; i < positions.size(); i++) {
            world.setState(positions.get(i), materials.get(i % materials.size()).getDefaultState());
        }
        world.setState(ORIGIN.add(2, 2, 2), BlockTypes.PRISMARINE.getDefaultState());
        world.setState(ORIGIN.add(2, 1, 1), BlockTypes.PRISMARINE.getDefaultState());

        ConduitStructure structure = scan(world);

        assertEquals(42, structure.frameBlocks().size());
        assertEquals(Set.copyOf(positions), Set.copyOf(structure.frameBlocks()));
        assertTrue(structure.isActive());
        assertTrue(structure.isHunting());
        assertEquals(96, structure.range());
        assertThrows(UnsupportedOperationException.class, () -> structure.frameBlocks().clear());
    }

    @Test
    void activationAndRangeFollowTheFrameCount() {
        List<Vector3i> positions = framePositions();
        for (int count : List.of(0, 7, 15, 16, 20, 21, 28, 35, 41, 42)) {
            BlockTestWorld world = waterWorld();
            for (int i = 0; i < count; i++) {
                world.setState(positions.get(i), BlockTypes.PRISMARINE.getDefaultState());
            }

            ConduitStructure structure = scan(world);

            assertEquals(count, structure.frameBlocks().size());
            assertEquals(count >= 16, structure.isActive());
            assertEquals(count == 42, structure.isHunting());
            assertEquals(count >= 16 ? count / 7 * 16 : 0, structure.range());
        }
    }

    @Test
    void acceptsContainedAndFlowingWaterButNotAirOrLava() {
        BlockTestWorld world = completeFrame();
        Vector3i neighbor = ORIGIN.add(1, 0, 0);
        world.setState(neighbor, BlockTypes.OAK_SLAB.getDefaultState());
        world.setLiquid(neighbor, LiquidState.of(BlockStates.WATER));
        assertTrue(scan(world).isHunting());

        world.setLiquid(neighbor, LiquidState.empty());
        world.setState(neighbor, BlockStates.AIR);
        assertFalse(scan(world).isActive());

        world.setLiquid(neighbor, LiquidState.of(BlockStates.LAVA));
        assertFalse(scan(world).isActive());

        world.setLiquid(neighbor, LiquidState.of(BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 3)));
        assertTrue(scan(world).isHunting());
    }

    @Test
    void doesNotCountStairsOrActivateAcrossUnloadedBlocks() {
        BlockTestWorld world = completeFrame();
        Vector3i frame = framePositions().getFirst();
        world.setState(frame, BlockTypes.PRISMARINE_STAIRS.getDefaultState());

        ConduitStructure partial = scan(world);
        assertEquals(41, partial.frameBlocks().size());
        assertTrue(partial.isActive());
        assertFalse(partial.isHunting());

        world.unload(frame);
        assertFalse(scan(world).isActive());
    }

    private static ConduitStructure scan(BlockTestWorld world) {
        return ConduitStructure.scan(world.block(ORIGIN).getLevel(), ORIGIN);
    }

    private static BlockTestWorld waterWorld() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    world.setState(ORIGIN.add(x, y, z), BlockStates.WATER);
                }
            }
        }

        world.setState(ORIGIN, BlockTypes.CONDUIT.getDefaultState());
        world.setLiquid(ORIGIN, LiquidState.of(BlockStates.WATER));
        return world;
    }

    private static BlockTestWorld completeFrame() {
        BlockTestWorld world = waterWorld();
        for (Vector3i position : framePositions()) {
            world.setState(position, BlockTypes.PRISMARINE.getDefaultState());
        }

        return world;
    }

    private static List<Vector3i> framePositions() {
        Set<Vector3i> positions = new LinkedHashSet<>();
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                if (Math.abs(a) == 2 || Math.abs(b) == 2) {
                    positions.add(ORIGIN.add(a, b, 0));
                    positions.add(ORIGIN.add(a, 0, b));
                    positions.add(ORIGIN.add(0, a, b));
                }
            }
        }

        return List.copyOf(positions);
    }
}
