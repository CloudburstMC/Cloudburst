package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.IceBlockHandlers;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.ChunkLightView;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.LongFunction;

import static org.junit.jupiter.api.Assertions.*;

class BlockLightEngineTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i SOURCE = Vector3i.from(0, 2, 0);
    private static final LightStateCache STATES = new LightStateCache(REGISTRY);

    @Test
    void complementarySlabFacesSealACorridorAndShapeChangesRelightIt() {
        LightTestGrid grid = grid();
        grid.minX = 0;
        grid.maxX = 4;
        grid.minZ = 0;
        grid.maxZ = 0;

        for (int x = 0; x <= 4; x++) {
            for (int y = 0; y < 5; y++) {
                grid.blocks.put(Vector3i.from(x, y, 0), BlockTypes.STONE.getDefaultState());
            }
        }
        BlockState bottom = BlockTypes.OAK_SLAB.getDefaultState().withTrait(BlockTraits.SLAB_SLOT, SlabSlot.BOTTOM);
        BlockState top = bottom.withTrait(BlockTraits.SLAB_SLOT, SlabSlot.TOP);
        Vector3i first = SOURCE.add(1, 0, 0);
        Vector3i second = SOURCE.add(2, 0, 0);
        Vector3i beyond = SOURCE.add(3, 0, 0);
        grid.blocks.put(SOURCE, BlockStates.LAVA);
        grid.blocks.put(first, bottom);
        grid.blocks.put(second, top);
        grid.blocks.put(beyond, BlockStates.AIR);

        grid.updateBlock(List.of(SOURCE));

        assertEquals(14, grid.lightAt(first));
        assertEquals(0, grid.lightAt(second));
        assertEquals(0, grid.lightAt(beyond));

        grid.blocks.put(second, bottom);
        grid.updateBlock(List.of(second));

        assertEquals(13, grid.lightAt(second));
        assertEquals(12, grid.lightAt(beyond));

        grid.blocks.put(second, top);
        grid.updateBlock(List.of(second));

        assertEquals(0, grid.lightAt(second));
        assertEquals(0, grid.lightAt(beyond));
    }

    @Test
    void initializationFindsBothLayerSourcesAndReplacesStaleLight() {
        LightTestGrid grid = grid();
        grid.minX = 0;
        grid.maxX = 15;
        grid.minZ = 0;
        grid.maxZ = 15;
        Vector3i primary = Vector3i.from(3, 2, 3);
        Vector3i secondary = Vector3i.from(10, 2, 10);
        Vector3i stale = Vector3i.from(8, 2, 5);
        grid.blocks.put(primary, BlockStates.LAVA);
        grid.secondary.put(secondary, BlockTypes.SEA_LANTERN.getDefaultState());
        grid.lights.put(stale, 15);

        grid.initializeBlock(0);

        assertEquals(15, grid.lightAt(primary));
        assertEquals(15, grid.lightAt(secondary));
        assertEquals(14, grid.lightAt(primary.add(1, 0, 0)));
        assertEquals(8, grid.lightAt(stale));
    }

    @Test
    void newlyLoadedBordersReceiveLightAndUnloadedSourcesStopContributing() {
        LightTestGrid grid = grid();
        grid.minX = 0;
        grid.maxX = 15;
        grid.minZ = 0;
        grid.maxZ = 15;
        Vector3i source = Vector3i.from(15, 2, 8);
        Vector3i neighbor = source.add(1, 0, 0);
        grid.blocks.put(source, BlockStates.LAVA);
        grid.initializeBlock(0);
        assertFalse(grid.lights.containsKey(neighbor));

        grid.maxX = 31;
        grid.initializeBlock(1);

        assertEquals(14, grid.lightAt(neighbor));
        assertEquals(10, grid.lightAt(neighbor.add(4, 0, 0)));

        grid.minX = 16;
        grid.updateBlock(List.of(source));

        assertEquals(0, grid.lightAt(neighbor));
        assertEquals(0, grid.lightAt(neighbor.add(4, 0, 0)));
    }

    @Test
    void opaqueLavaEmitsLightAndLightsIceEnoughToMelt() {
        LightTestGrid grid = grid();
        Vector3i ice = SOURCE.add(1, 0, 0);
        grid.blocks.put(SOURCE, BlockStates.LAVA);
        grid.blocks.put(ice, BlockTypes.ICE.getDefaultState());

        grid.updateBlock(List.of(SOURCE));

        int attenuation = Math.max(1, grid.blocks.get(ice).getLightDampening());
        assertEquals(15, grid.lightAt(SOURCE));
        assertEquals(15 - attenuation, grid.lightAt(ice));
        assertEquals(14, grid.lightAt(SOURCE.add(-1, 0, 0)));

        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(ice, grid.blocks.get(ice));
        IceBlockHandlers.randomTick(world.block(ice), grid.lightAt(ice), false);
        assertSame(BlockStates.WATER, world.state(ice));
    }

    @Test
    void removingOneSourcePreservesLightFromAnotherAndRemovingBothClearsIt() {
        LightTestGrid grid = grid();
        Vector3i second = SOURCE.add(4, 0, 0);
        grid.blocks.put(SOURCE, BlockStates.LAVA);
        grid.blocks.put(second, BlockTypes.TORCH.getDefaultState());
        grid.updateBlock(List.of(SOURCE, second));

        grid.blocks.remove(SOURCE);
        grid.updateBlock(List.of(SOURCE));
        assertEquals(14, grid.lightAt(second));
        assertEquals(10, grid.lightAt(SOURCE));

        grid.blocks.remove(second);
        grid.updateBlock(List.of(second));
        assertTrue(grid.lights.values().stream().allMatch(light -> light == 0));
    }

    @Test
    void opacityChangesRecalculateIncomingLightAndSecondaryLayersCanEmit() {
        LightTestGrid grid = grid();
        grid.secondary.put(SOURCE, BlockTypes.GLOWSTONE.getDefaultState());
        Vector3i target = SOURCE.add(1, 0, 0);
        grid.updateBlock(List.of(SOURCE));
        assertEquals(15, grid.lightAt(SOURCE));
        assertEquals(14, grid.lightAt(target));

        grid.blocks.put(target, BlockStates.STONE);
        grid.updateBlock(List.of(target));
        assertEquals(0, grid.lightAt(target));

        grid.blocks.remove(target);
        grid.updateBlock(List.of(target));
        assertEquals(14, grid.lightAt(target));
    }

    @Test
    void chunkAccessUsesAbsoluteNegativeCoordinatesAndOnlyLoadedChunks() {
        Level level = TestLevels.level(0, 16);
        CloudChunk chunk = new CloudChunk(-2, 1, level);
        Vector3i source = Vector3i.from(-17, 2, 17);

        try (LockedChunk locked = chunk.lockForWrite()) {
            locked.setBlockState(15, 2, 1, BlockLayer.PRIMARY, BlockStates.LAVA);
            LongFunction<ChunkLightView> views = key -> key == CloudChunk.key(-2, 1) ? locked : null;
            ChunkLightAccess access = new ChunkLightAccess(LightChannel.BLOCK, 0, 16, STATES, views);
            LightPropagation propagation = new LightPropagation(LightChannel.BLOCK);
            propagation.invalidate(access, LightPositions.pack(source.getX(), source.getY(), source.getZ()));
            LightTestGrid.drain(access, propagation);

            assertEquals(15, access.light(source.getX(), source.getY(), source.getZ()));
            assertEquals(14, access.light(source.getX() - 1, source.getY(), source.getZ()));
            assertEquals(0, access.light(source.getX() + 1, source.getY(), source.getZ()));
            assertEquals(15, locked.getBlockLight(15, 2, 1));
        }
    }

    private static LightTestGrid grid() {
        return new LightTestGrid(-8, 8, -8, 8, 0, 5);
    }
}
