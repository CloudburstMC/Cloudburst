package org.cloudburstmc.server.level.light;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.IceBlockHandlers;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockLightEngineTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i SOURCE = Vector3i.from(0, 2, 0);

    @Test
    void complementarySlabFacesSealACorridorAndShapeChangesRelightIt() {
        Grid grid = new Grid();
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

        BlockLightEngine.update(grid, List.of(SOURCE));

        assertEquals(14, grid.light(first));
        assertEquals(0, grid.light(second));
        assertEquals(0, grid.light(beyond));

        grid.blocks.put(second, bottom);
        BlockLightEngine.update(grid, List.of(second));

        assertEquals(13, grid.light(second));
        assertEquals(12, grid.light(beyond));

        grid.blocks.put(second, top);
        BlockLightEngine.update(grid, List.of(second));

        assertEquals(0, grid.light(second));
        assertEquals(0, grid.light(beyond));
    }

    @Test
    void initializationFindsBothLayerSourcesAndReplacesStaleLight() {
        Grid grid = new Grid();
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

        BlockLightEngine.update(grid, BlockLightEngine.initialize(grid, 0, 0));

        assertEquals(15, grid.light(primary));
        assertEquals(15, grid.light(secondary));
        assertEquals(14, grid.light(primary.add(1, 0, 0)));
        assertEquals(8, grid.light(stale));
    }

    @Test
    void newlyLoadedBordersReceiveLightAndUnloadedSourcesStopContributing() {
        Grid grid = new Grid();
        grid.minX = 0;
        grid.maxX = 15;
        grid.minZ = 0;
        grid.maxZ = 15;
        Vector3i source = Vector3i.from(15, 2, 8);
        Vector3i neighbor = source.add(1, 0, 0);
        grid.blocks.put(source, BlockStates.LAVA);
        BlockLightEngine.update(grid, BlockLightEngine.initialize(grid, 0, 0));
        assertFalse(grid.lights.containsKey(neighbor));

        grid.maxX = 31;
        BlockLightEngine.update(grid, BlockLightEngine.initialize(grid, 1, 0));

        assertEquals(14, grid.light(neighbor));
        assertEquals(10, grid.light(neighbor.add(4, 0, 0)));

        grid.minX = 16;
        BlockLightEngine.update(grid, List.of(source));

        assertEquals(0, grid.light(neighbor));
        assertEquals(0, grid.light(neighbor.add(4, 0, 0)));
    }

    @Test
    void opaqueLavaEmitsLightAndLightsIceEnoughToMelt() {
        Grid grid = grid();
        Vector3i ice = SOURCE.add(1, 0, 0);
        grid.blocks.put(SOURCE, BlockStates.LAVA);
        grid.blocks.put(ice, BlockTypes.ICE.getDefaultState());

        BlockLightEngine.update(grid, List.of(SOURCE));

        int attenuation = Math.max(1, grid.blocks.get(ice).getLightDampening());
        assertEquals(15, grid.light(SOURCE));
        assertEquals(15 - attenuation, grid.light(ice));
        assertEquals(14, grid.light(SOURCE.add(-1, 0, 0)));

        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(ice, grid.blocks.get(ice));
        IceBlockHandlers.randomTick(world.block(ice), grid.light(ice), false);
        assertSame(BlockStates.WATER, world.state(ice));
    }

    @Test
    void removingOneSourcePreservesLightFromAnotherAndRemovingBothClearsIt() {
        Grid grid = grid();
        Vector3i second = SOURCE.add(4, 0, 0);
        grid.blocks.put(SOURCE, BlockStates.LAVA);
        grid.blocks.put(second, BlockTypes.TORCH.getDefaultState());
        BlockLightEngine.update(grid, List.of(SOURCE, second));

        grid.blocks.remove(SOURCE);
        BlockLightEngine.update(grid, List.of(SOURCE));
        assertEquals(14, grid.light(second));
        assertEquals(10, grid.light(SOURCE));

        grid.blocks.remove(second);
        BlockLightEngine.update(grid, List.of(second));
        assertTrue(grid.lights.values().stream().allMatch(light -> light == 0));
    }

    @Test
    void opacityChangesRecalculateIncomingLightAndSecondaryLayersCanEmit() {
        Grid grid = grid();
        grid.secondary.put(SOURCE, BlockTypes.GLOWSTONE.getDefaultState());
        Vector3i target = SOURCE.add(1, 0, 0);
        BlockLightEngine.update(grid, List.of(SOURCE));
        assertEquals(15, grid.light(SOURCE));
        assertEquals(14, grid.light(target));

        grid.blocks.put(target, BlockStates.STONE);
        BlockLightEngine.update(grid, List.of(target));
        assertEquals(0, grid.light(target));

        grid.blocks.remove(target);
        BlockLightEngine.update(grid, List.of(target));
        assertEquals(14, grid.light(target));
    }

    @Test
    void levelAdapterUsesAbsoluteNegativeCoordinatesAndOnlyLoadedChunks() {
        Grid grid = grid();
        Vector3i source = Vector3i.from(-17, 2, 17);
        grid.blocks.put(source, BlockStates.LAVA);
        Chunk chunk = InterfaceProxy.create(Chunk.class, (proxy, method, arguments) -> {
            Vector3i position = Vector3i.from(-32 + (Integer) arguments[0], (Integer) arguments[1], 16 + (Integer) arguments[2]);
            return switch (method.getName()) {
                case "getBlockState" -> ((BlockLayer) arguments[3] == BlockLayer.PRIMARY ? grid.blocks : grid.secondary)
                        .getOrDefault(position, BlockStates.AIR);
                case "getBlockLight" -> grid.light(position);
                case "setBlockLight" -> {
                    grid.lights.put(position, (Integer) arguments[3]);
                    yield null;
                }
                default -> throw new AssertionError("Unexpected chunk access: " + method);
            };
        });
        Level level = InterfaceProxy.create(Level.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getServer" -> InterfaceProxy.create(Server.class, (server, serverMethod, serverArguments) -> {
                if (serverMethod.getName().equals("getBlockRegistry")) {
                    return REGISTRY;
                }

                throw new AssertionError("Unexpected server access: " + serverMethod);
            });
            case "getMinHeight" -> 0;
            case "getMaxHeight" -> 5;
            case "getLoadedChunk" -> (Integer) arguments[0] == -2 && (Integer) arguments[1] == 1 ? chunk : null;
            default -> throw new AssertionError("Unexpected level access: " + method);
        });

        BlockLightEngine.update(level, List.of(source));

        assertEquals(15, grid.light(source));
        assertEquals(14, grid.light(source.add(-1, 0, 0)));
        assertEquals(0, grid.light(source.add(1, 0, 0)));
        assertFalse(grid.lights.containsKey(Vector3i.from(15, 2, 1)));
    }

    private static Grid grid() {
        return new Grid();
    }

    private static class Grid implements LightAccess {
        private final Map<Vector3i, BlockState> blocks = new HashMap<>();
        private final Map<Vector3i, BlockState> secondary = new HashMap<>();
        private final Map<Vector3i, Integer> lights = new HashMap<>();
        private int minX = -8;
        private int maxX = 8;
        private int minZ = -8;
        private int maxZ = 8;

        @Override
        public CloudBlockRegistry registry() {
            return REGISTRY;
        }

        @Override
        public LightAccess.@Nullable Sample read(Vector3i position) {
            if (position.getX() < this.minX || position.getX() > this.maxX
                    || position.getZ() < this.minZ || position.getZ() > this.maxZ
                    || position.getY() < 0 || position.getY() > 4) {
                return null;
            }
            return new LightAccess.Sample(this.blocks.getOrDefault(position, BlockStates.AIR),
                    this.secondary.getOrDefault(position, BlockStates.AIR), this.light(position));
        }

        @Override
        public void write(Vector3i position, int light) {
            this.lights.put(position, light);
        }

        @Override
        public int minHeight() {
            return 0;
        }

        @Override
        public int maxHeight() {
            return 5;
        }

        private int light(Vector3i position) {
            return this.lights.getOrDefault(position, 0);
        }
    }
}
