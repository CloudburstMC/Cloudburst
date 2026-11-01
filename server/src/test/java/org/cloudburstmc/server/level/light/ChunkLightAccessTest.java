package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.Test;

import java.util.function.LongFunction;

import static org.junit.jupiter.api.Assertions.*;

class ChunkLightAccessTest {

    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    @Test
    void skyLightCreatesMissingSectionsWithoutDirtyingTheChunk() {
        Level level = TestLevels.level(MIN_HEIGHT, MAX_HEIGHT);
        CloudChunk chunk = new CloudChunk(0, 0, level);
        try (LockedChunk locked = chunk.lockForWrite()) {
            locked.setBlockState(0, MIN_HEIGHT, 0, BlockLayer.PRIMARY,
                    BlockTypes.BEDROCK.getDefaultState());
        }
        chunk.advanceGenerationStatus(ChunkGenerationStatus.GENERATED);
        chunk.acknowledgeSave(chunk.captureSaveRevision());
        assertFalse(chunk.isDirty());

        try (LockedChunk locked = chunk.lockForWrite()) {
            LightStateCache states = new LightStateCache(BlockTestRegistry.get());
            ChunkLightAccess access = new ChunkLightAccess(LightChannel.SKY,
                    MIN_HEIGHT, MAX_HEIGHT,
                    states, key -> key == chunk.getKey() ? locked : null);
            LightPropagation propagation = new LightPropagation(LightChannel.SKY);
            SkyLightEngine.initialize(access, 0, 0, propagation);
            assertTrue(propagation.run(access, Integer.MAX_VALUE), "sky propagation did not drain");
        }

        assertEquals(15, chunk.getSkyLight(0, 300, 0));
        try (LockedChunk locked = chunk.lockForRead()) {
            assertNotNull(locked.getSection((300 - MIN_HEIGHT) >> 4));
        }
        assertFalse(chunk.isDirty());
    }

    @Test
    void bothLightChannelsUseAbsoluteCoordinatesAndCacheOnlyWithinThePass() {
        BlockState water = BlockTestRegistry.get().getBlock(BlockTypes.WATER);
        for (LightChannel channel : LightChannel.values()) {
            Level level = TestLevels.level(MIN_HEIGHT, MAX_HEIGHT);
            CloudChunk chunk = new CloudChunk(-2, 1, level);
            try (LockedChunk locked = chunk.lockForWrite()) {
                locked.setBlockState(15, 64, 1, BlockLayer.PRIMARY, BlockStates.AIR);
                locked.setBlockState(15, 64, 1, BlockLayer.SECONDARY, water);
                locked.setBlockLight(15, 64, 1, 7);
                locked.setSkyLight(15, 64, 1, 12);
                int[] lookups = {0};
                LongFunction<LockedChunk> views = key -> {
                    lookups[0]++;
                    return key == CloudChunk.key(-2, 1) ? locked : null;
                };
                ChunkLightAccess access = new ChunkLightAccess(channel,
                        MIN_HEIGHT, MAX_HEIGHT,
                        new LightStateCache(BlockTestRegistry.get()), views);

                Vector3i position = Vector3i.from(-17, 64, 17);
                assertEquals(BlockStates.AIR, access.primary(position.getX(), position.getY(), position.getZ()));
                assertEquals(water, access.secondary(position.getX(), position.getY(), position.getZ()));
                assertEquals(channel == LightChannel.BLOCK ? 7 : 12,
                        access.light(position.getX(), position.getY(), position.getZ()));
                access.setLight(position.getX(), position.getY(), position.getZ(), 9);
                assertEquals(9, channel == LightChannel.BLOCK
                        ? locked.getBlockLight(15, 64, 1) : locked.getSkyLight(15, 64, 1));
                assertEquals(1, lookups[0]);

                Vector3i unloaded = Vector3i.from(0, 64, 0);
                assertFalse(access.isLoaded(unloaded.getX(), unloaded.getZ()));
                assertEquals(BlockStates.AIR, access.primary(unloaded.getX(), unloaded.getY(), unloaded.getZ()));
                assertEquals(BlockStates.AIR, access.secondary(unloaded.getX(), unloaded.getY(), unloaded.getZ()));
                assertEquals(0, access.light(unloaded.getX(), unloaded.getY(), unloaded.getZ()));
                access.setLight(unloaded.getX(), unloaded.getY(), unloaded.getZ(), 10);
                assertEquals(0, access.light(unloaded.getX(), unloaded.getY(), unloaded.getZ()));
                assertEquals(2, lookups[0]);

                assertEquals(0, access.light(position.getX(), 320, position.getZ()));
                access.setLight(position.getX(), -65, position.getZ(), 10);
                assertEquals(2, lookups[0]);

                ChunkLightAccess fresh = new ChunkLightAccess(channel,
                        MIN_HEIGHT, MAX_HEIGHT,
                        new LightStateCache(BlockTestRegistry.get()), views);
                assertEquals(9, fresh.light(position.getX(), position.getY(), position.getZ()));
                assertEquals(3, lookups[0]);
            }
        }
    }
}
