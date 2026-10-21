package org.cloudburstmc.server.level.light;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.FrostedIceBlockHandlers;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SkyLightEngineTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i ICE = Vector3i.from(8, 2, 8);

    @Test
    void slabFacesStopDirectSkyAndShapeChangesUpdateTheColumn() {
        Grid grid = new Grid();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 4; y++) {
                    grid.blocks.put(Vector3i.from(x, y, z), BlockTypes.STONE.getDefaultState());
                }
            }
        }
        Vector3i slab = Vector3i.from(8, 3, 8);
        BlockState bottom = BlockTypes.OAK_SLAB.getDefaultState().withTrait(BlockTraits.SLAB_SLOT, SlabSlot.BOTTOM);
        grid.blocks.put(slab, bottom);
        grid.blocks.put(slab.add(0, -1, 0), BlockStates.AIR);
        initialize(grid);

        assertEquals(15, grid.light(slab));
        assertEquals(0, grid.light(slab.add(0, -1, 0)));

        grid.blocks.put(slab, bottom.withTrait(BlockTraits.SLAB_SLOT, SlabSlot.TOP));
        SkyLightEngine.update(grid, List.of(slab));

        assertEquals(0, grid.light(slab));
        assertEquals(0, grid.light(slab.add(0, -1, 0)));

        grid.blocks.put(slab, BlockStates.AIR);
        SkyLightEngine.update(grid, List.of(slab));

        assertEquals(15, grid.light(slab.add(0, -1, 0)));
    }

    @Test
    void initializationLightsOpenColumnsAndAttenuatesFrostedIce() {
        Grid grid = new Grid();
        BlockState ice = BlockTypes.FROSTED_ICE.getDefaultState();
        grid.blocks.put(ICE, ice);

        initialize(grid);

        assertEquals(15, grid.light(ICE.add(0, 1, 0)));
        assertEquals(15 - ice.getLightDampening(), grid.light(ICE));
        assertEquals(15, grid.light(Vector3i.from(0, 0, 0)));
        assertEquals(14, grid.light(ICE.add(0, -1, 0)));
    }

    @Test
    void daylightAgesFrostedIceThroughVisibleCracksAndIntoWater() {
        Grid grid = new Grid();
        grid.blocks.put(ICE, BlockTypes.FROSTED_ICE.getDefaultState());
        initialize(grid);
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(ICE, grid.blocks.get(ICE));
        int brightness = grid.light(ICE) - SkyLightEngine.skyDarkness(DayCycle.NOON, 0, 0);

        for (int age = 1; age <= 3; age++) {
            FrostedIceBlockHandlers.tick(world.block(ICE), new Random(0), brightness, false);
            assertEquals(age, world.state(ICE).ensureTrait(BlockTraits.AGE));
        }

        FrostedIceBlockHandlers.tick(world.block(ICE), new Random(0), brightness, false);
        assertSame(BlockStates.WATER, world.state(ICE));
    }

    @Test
    void aRoofBlocksSkylightAndRemovingAndReplacingItUpdatesTheWholeShadow() {
        Grid grid = new Grid();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                grid.blocks.put(Vector3i.from(x, 2, z), BlockStates.STONE);
            }
        }
        initialize(grid);
        Vector3i below = ICE.add(0, -1, 0);
        assertEquals(0, grid.light(below));

        grid.blocks.remove(ICE);
        SkyLightEngine.update(grid, List.of(ICE));

        assertEquals(15, grid.light(below));
        assertEquals(14, grid.light(below.add(1, 0, 0)));

        grid.blocks.put(ICE, BlockStates.STONE);
        SkyLightEngine.update(grid, List.of(ICE));

        assertEquals(0, grid.light(below));
        assertEquals(0, grid.light(below.add(1, 0, 0)));
    }

    @Test
    void containedWaterContributesOpacityAndRemovingItRestoresFullSky() {
        Grid grid = new Grid();
        grid.blocks.put(ICE, BlockTypes.MANGROVE_ROOTS.getDefaultState());
        grid.secondary.put(ICE, BlockStates.WATER);
        initialize(grid);

        assertEquals(15 - BlockStates.WATER.getLightDampening(), grid.light(ICE));

        grid.secondary.remove(ICE);
        SkyLightEngine.update(grid, List.of(ICE));

        assertEquals(15, grid.light(ICE));
    }

    @Test
    void propagationDoesNotInventSourcesAtUnloadedHorizontalBoundaries() {
        Grid grid = new Grid();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                grid.blocks.put(Vector3i.from(x, 3, z), BlockStates.STONE);
            }
        }
        initialize(grid);

        assertEquals(0, grid.light(Vector3i.from(0, 2, 0)));
        assertEquals(0, grid.light(Vector3i.from(15, 2, 15)));
        assertTrue(grid.lights.keySet().stream().allMatch(Grid::contains));
    }

    private static void initialize(Grid grid) {
        SkyLightEngine.update(grid, SkyLightEngine.initialize(grid, 0, 0));
    }

    private static class Grid implements LightAccess {

        private final Map<Vector3i, BlockState> blocks = new HashMap<>();
        private final Map<Vector3i, BlockState> secondary = new HashMap<>();
        private final Map<Vector3i, Integer> lights = new HashMap<>();

        @Override
        public CloudBlockRegistry registry() {
            return REGISTRY;
        }

        @Override
        public int minHeight() {
            return 0;
        }

        @Override
        public int maxHeight() {
            return 4;
        }

        @Override
        public LightAccess.@Nullable Sample read(Vector3i position) {
            if (!contains(position)) {
                return null;
            }

            return new LightAccess.Sample(this.blocks.getOrDefault(position, BlockStates.AIR),
                    this.secondary.getOrDefault(position, BlockStates.AIR), this.light(position));
        }

        @Override
        public void write(Vector3i position, int light) {
            assertTrue(contains(position));
            this.lights.put(position, light);
        }

        private int light(Vector3i position) {
            return this.lights.getOrDefault(position, 0);
        }

        private static boolean contains(Vector3i position) {
            return position.getX() >= 0 && position.getX() < 16
                    && position.getZ() >= 0 && position.getZ() < 16
                    && position.getY() >= 0 && position.getY() < 4;
        }
    }
}
