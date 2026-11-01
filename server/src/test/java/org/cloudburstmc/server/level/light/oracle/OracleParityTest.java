package org.cloudburstmc.server.level.light.oracle;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.level.light.BlockLightEngine;
import org.cloudburstmc.server.level.light.ChunkLightAccess;
import org.cloudburstmc.server.level.light.LightAccess;
import org.cloudburstmc.server.level.light.LightTestChunks;
import org.cloudburstmc.server.level.light.SkyLightEngine;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OracleParityTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void oracleMatchesTheCurrentEngine() {
        Long2ObjectMap<CloudChunk> oracleChunks = LightTestChunks.overworld(7L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> mainChunks = LightTestChunks.overworld(7L, 0, 0, 3);
        int minHeight = DimensionHeight.OVERWORLD.getMinHeight();
        int maxHeight = DimensionHeight.OVERWORLD.getMaxHeight();

        for (long key : sortedKeys(oracleChunks)) {
            OracleLight.initialize(REGISTRY, minHeight, maxHeight, oracleChunks::get, oracleChunks.get(key));
        }

        Level level = levelOver(mainChunks, minHeight, maxHeight);
        for (long key : sortedKeys(mainChunks)) {
            CloudChunk chunk = mainChunks.get(key);
            List<Vector3i> blockUpdates;
            List<Vector3i> skyUpdates;
            try (LockedChunk locked = chunk.lockForWrite()) {
                blockUpdates = BlockLightEngine.initialize(new ChunkLightAccess(level, LightAccess.Type.BLOCK, locked), chunk.getX(), chunk.getZ());
                skyUpdates = SkyLightEngine.initialize(new ChunkLightAccess(level, LightAccess.Type.SKY, locked), chunk.getX(), chunk.getZ());
            }
            BlockLightEngine.update(level, blockUpdates);
            SkyLightEngine.update(level, skyUpdates);
        }

        LightTestChunks.assertSameLight(oracleChunks, mainChunks);

        CloudChunk oracleCenter = oracleChunks.get(CloudChunk.key(1, 1));
        CloudChunk mainCenter = mainChunks.get(CloudChunk.key(1, 1));
        Vector3i roof = roofBlock(oracleCenter, minHeight, maxHeight);
        assertEquals(oracleCenter.getBlockState(roof.getX() & 15, roof.getY(), roof.getZ() & 15, BlockLayer.PRIMARY),
                mainCenter.getBlockState(roof.getX() & 15, roof.getY(), roof.getZ() & 15, BlockLayer.PRIMARY));
        Vector3i lamp = roof.add(0, 1, 0);
        assertSame(BlockTypes.AIR, oracleCenter.getBlockState(lamp.getX() & 15, lamp.getY(), lamp.getZ() & 15, BlockLayer.PRIMARY).getType());
        assertSame(BlockTypes.AIR, mainCenter.getBlockState(lamp.getX() & 15, lamp.getY(), lamp.getZ() & 15, BlockLayer.PRIMARY).getType());
        for (CloudChunk center : new CloudChunk[]{oracleCenter, mainCenter}) {
            center.setBlockState(roof.getX() & 15, roof.getY(), roof.getZ() & 15, BlockLayer.PRIMARY, BlockStates.AIR);
            center.setBlockState(lamp.getX() & 15, lamp.getY(), lamp.getZ() & 15, BlockLayer.PRIMARY, BlockStates.GLOWSTONE);
        }

        List<Vector3i> changed = List.of(roof, lamp);
        OracleLight.update(REGISTRY, minHeight, maxHeight, oracleChunks::get, changed);
        BlockLightEngine.update(level, changed);
        SkyLightEngine.update(level, changed);

        LightTestChunks.assertSameLight(oracleChunks, mainChunks);
    }

    private static Vector3i roofBlock(CloudChunk chunk, int minHeight, int maxHeight) {
        for (int y = maxHeight - 1; y >= minHeight; y--) {
            if (chunk.getBlockState(8, y, 8, BlockLayer.PRIMARY).getType() != BlockTypes.AIR) {
                assertTrue(y + 1 < maxHeight);
                return Vector3i.from((chunk.getX() << 4) + 8, y, (chunk.getZ() << 4) + 8);
            }
        }
        throw new AssertionError("Centre column has no roof block");
    }

    private static Level levelOver(Long2ObjectMap<CloudChunk> chunks, int minHeight, int maxHeight) {
        return InterfaceProxy.create(Level.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getServer" -> InterfaceProxy.create(Server.class, (server, serverMethod, serverArguments) -> {
                if (serverMethod.getName().equals("getBlockRegistry")) {
                    return REGISTRY;
                }

                throw new AssertionError("Unexpected server access: " + serverMethod);
            });
            case "getMinHeight" -> minHeight;
            case "getMaxHeight" -> maxHeight;
            case "getLoadedChunk" -> {
                if (arguments.length == 2) {
                    yield chunks.get(CloudChunk.key((Integer) arguments[0], (Integer) arguments[1]));
                }

                yield chunks.get((long) (Long) arguments[0]);
            }
            default -> throw new AssertionError("Unexpected level access: " + method);
        });
    }

    private static long[] sortedKeys(Long2ObjectMap<CloudChunk> chunks) {
        long[] keys = chunks.keySet().toLongArray();
        Arrays.sort(keys);
        return keys;
    }
}
