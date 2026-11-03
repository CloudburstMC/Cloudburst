package org.cloudburstmc.server.level.light;

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

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SkyLightEngineTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i ICE = Vector3i.from(8, 2, 8);

    @Test
    void slabFacesStopDirectSkyAndShapeChangesUpdateTheColumn() {
        LightTestGrid grid = new LightTestGrid(0, 15, 0, 15, 0, 4);

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
        grid.initializeSky();

        assertEquals(15, grid.lightAt(slab));
        assertEquals(0, grid.lightAt(slab.add(0, -1, 0)));

        grid.blocks.put(slab, bottom.withTrait(BlockTraits.SLAB_SLOT, SlabSlot.TOP));
        grid.updateSky(List.of(slab));

        assertEquals(0, grid.lightAt(slab));
        assertEquals(0, grid.lightAt(slab.add(0, -1, 0)));

        grid.blocks.put(slab, BlockStates.AIR);
        grid.updateSky(List.of(slab));

        assertEquals(15, grid.lightAt(slab.add(0, -1, 0)));
    }

    @Test
    void initializationLightsOpenColumnsAndAttenuatesFrostedIce() {
        LightTestGrid grid = new LightTestGrid(0, 15, 0, 15, 0, 4);
        BlockState ice = BlockTypes.FROSTED_ICE.getDefaultState();
        grid.blocks.put(ICE, ice);

        grid.initializeSky();

        assertEquals(15, grid.lightAt(ICE.add(0, 1, 0)));
        assertEquals(15 - ice.getLightDampening(), grid.lightAt(ICE));
        assertEquals(15, grid.lightAt(Vector3i.from(0, 0, 0)));
        assertEquals(14, grid.lightAt(ICE.add(0, -1, 0)));
    }

    @Test
    void daylightAgesFrostedIceThroughVisibleCracksAndIntoWater() {
        LightTestGrid grid = new LightTestGrid(0, 15, 0, 15, 0, 4);
        grid.blocks.put(ICE, BlockTypes.FROSTED_ICE.getDefaultState());
        grid.initializeSky();
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(ICE, grid.blocks.get(ICE));
        int brightness = grid.lightAt(ICE) - SkyLightEngine.skyDarkness(DayCycle.NOON, 0, 0);

        for (int age = 1; age <= 3; age++) {
            FrostedIceBlockHandlers.tick(world.block(ICE), new Random(0), brightness, false);
            assertEquals(age, world.state(ICE).ensureTrait(BlockTraits.AGE));
        }

        FrostedIceBlockHandlers.tick(world.block(ICE), new Random(0), brightness, false);
        assertSame(BlockStates.WATER, world.state(ICE));
    }

    @Test
    void aRoofBlocksSkylightAndRemovingAndReplacingItUpdatesTheWholeShadow() {
        LightTestGrid grid = new LightTestGrid(0, 15, 0, 15, 0, 4);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                grid.blocks.put(Vector3i.from(x, 2, z), BlockStates.STONE);
            }
        }

        grid.initializeSky();
        Vector3i below = ICE.add(0, -1, 0);
        assertEquals(0, grid.lightAt(below));

        grid.blocks.remove(ICE);
        grid.updateSky(List.of(ICE));

        assertEquals(15, grid.lightAt(below));
        assertEquals(14, grid.lightAt(below.add(1, 0, 0)));

        grid.blocks.put(ICE, BlockStates.STONE);
        grid.updateSky(List.of(ICE));

        assertEquals(0, grid.lightAt(below));
        assertEquals(0, grid.lightAt(below.add(1, 0, 0)));
    }

    @Test
    void containedWaterContributesOpacityAndRemovingItRestoresFullSky() {
        LightTestGrid grid = new LightTestGrid(0, 15, 0, 15, 0, 4);
        grid.blocks.put(ICE, BlockTypes.MANGROVE_ROOTS.getDefaultState());
        grid.secondary.put(ICE, BlockStates.WATER);
        grid.initializeSky();

        assertEquals(15 - BlockStates.WATER.getLightDampening(), grid.lightAt(ICE));

        grid.secondary.remove(ICE);
        grid.updateSky(List.of(ICE));

        assertEquals(15, grid.lightAt(ICE));
    }

    @Test
    void propagationDoesNotInventSourcesAtUnloadedHorizontalBoundaries() {
        LightTestGrid grid = new LightTestGrid(0, 15, 0, 15, 0, 4);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                grid.blocks.put(Vector3i.from(x, 3, z), BlockStates.STONE);
            }
        }

        grid.initializeSky();

        assertEquals(0, grid.lightAt(Vector3i.from(0, 2, 0)));
        assertEquals(0, grid.lightAt(Vector3i.from(15, 2, 15)));
    }
}
