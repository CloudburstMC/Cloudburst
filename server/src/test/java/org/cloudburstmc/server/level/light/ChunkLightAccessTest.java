package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

class ChunkLightAccessTest {

    @Test
    void bothLightChannelsUseAbsoluteCoordinatesAndCacheOnlyWithinThePass() {
        BlockState water = BlockTestRegistry.get().getBlock(BlockTypes.WATER);
        for (LightAccess.Type type : LightAccess.Type.values()) {
            int[] lookups = {0};
            int[] written = {-1};
            Chunk chunk = InterfaceProxy.create(Chunk.class, (proxy, method, arguments) -> {
                assertEquals(15, arguments[0]);
                assertEquals(64, arguments[1]);
                assertEquals(1, arguments[2]);
                return switch (method.getName()) {
                    case "getBlockState" -> arguments[3] == BlockLayer.PRIMARY ? BlockStates.AIR : water;
                    case "getBlockLight" -> {
                        assertEquals(LightAccess.Type.BLOCK, type);
                        yield 7;
                    }
                    case "getSkyLight" -> {
                        assertEquals(LightAccess.Type.SKY, type);
                        yield 12;
                    }
                    case "setBlockLight", "setSkyLight" -> {
                        assertEquals(type == LightAccess.Type.BLOCK ? "setBlockLight" : "setSkyLight", method.getName());
                        written[0] = (Integer) arguments[3];
                        yield null;
                    }
                    default -> throw new AssertionError("Unexpected chunk access: " + method);
                };
            });
            Level level = InterfaceProxy.create(Level.class, (proxy, method, arguments) -> switch (method.getName()) {
                case "getServer" -> InterfaceProxy.create(Server.class, (server, serverMethod, serverArguments) -> {
                    if (serverMethod.getName().equals("getBlockRegistry")) {
                        return BlockTestRegistry.get();
                    }

                    throw new AssertionError("Unexpected server access: " + serverMethod);
                });
                case "getMinHeight" -> -64;
                case "getMaxHeight" -> 320;
                case "getLoadedChunk" -> {
                    lookups[0]++;
                    yield (Integer) arguments[0] == -2 && (Integer) arguments[1] == 1 ? chunk : null;
                }
                default -> throw new AssertionError("Unexpected level access: " + method);
            });
            Vector3i position = Vector3i.from(-17, 64, 17);
            ChunkLightAccess access = new ChunkLightAccess(level, type);

            LightAccess.Sample sample = requireNonNull(access.read(position));
            assertSame(BlockStates.AIR, sample.primary());
            assertSame(water, sample.secondary());
            assertEquals(type == LightAccess.Type.BLOCK ? 7 : 12, sample.light());
            access.write(position, 9);
            assertEquals(9, written[0]);
            assertEquals(1, lookups[0]);

            Vector3i unloaded = Vector3i.from(0, 64, 0);
            assertNull(access.read(unloaded));
            assertNull(access.read(unloaded));
            access.write(unloaded, 10);
            assertEquals(2, lookups[0]);

            assertNull(access.read(position.add(0, 256, 0)));
            access.write(position.add(0, -129, 0), 10);
            assertEquals(2, lookups[0]);

            assertNotNull(new ChunkLightAccess(level, type).read(position));
            assertEquals(3, lookups[0]);
        }
    }
}
