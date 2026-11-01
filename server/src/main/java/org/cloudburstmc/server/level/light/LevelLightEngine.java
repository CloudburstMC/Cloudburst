package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongFunction;

@UtilityClass
public class LevelLightEngine {

    public static void initialize(CloudLevel level, CloudChunk chunk) {
        List<CloudChunk> surrounding = new ArrayList<>(9);
        surrounding.add(chunk);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                CloudChunk neighbour = level.getLoadedChunk(chunk.getX() + dx, chunk.getZ() + dz);
                if (neighbour != null && neighbour != chunk) {
                    surrounding.add(neighbour);
                }
            }
        }
        LockedChunk[] locked = lockSorted(surrounding);
        try {
            LightStateCache states = new LightStateCache(level.getServer().getBlockRegistry());
            LongFunction<LockedChunk> views = views(locked);
            int minHeight = level.getMinHeight();
            int maxHeight = level.getMaxHeight();
            LightPropagation block = new LightPropagation(LightChannel.BLOCK);
            ChunkLightAccess blockAccess = new ChunkLightAccess(LightChannel.BLOCK, minHeight, maxHeight, states, views);
            BlockLightEngine.initialize(blockAccess, chunk.getX(), chunk.getZ(), block);
            block.run(blockAccess, Integer.MAX_VALUE);
            if (level.getDimension() == CloudLevel.DIMENSION_OVERWORLD) {
                LightPropagation sky = new LightPropagation(LightChannel.SKY);
                ChunkLightAccess skyAccess = new ChunkLightAccess(LightChannel.SKY, minHeight, maxHeight, states, views);
                SkyLightEngine.initialize(skyAccess, chunk.getX(), chunk.getZ(), sky);
                sky.run(skyAccess, Integer.MAX_VALUE);
            }
        } finally {
            for (int i = locked.length - 1; i >= 0; i--) {
                locked[i].close();
            }
        }
    }

    public static void updateBoundary(CloudLevel level, int chunkX, int chunkZ) {
        List<CloudChunk> surrounding = new ArrayList<>(8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                CloudChunk neighbour = level.getLoadedChunk(chunkX + dx, chunkZ + dz);
                if (neighbour != null) {
                    surrounding.add(neighbour);
                }
            }
        }
        if (surrounding.isEmpty()) {
            return;
        }
        LockedChunk[] locked = lockSorted(surrounding);
        try {
            boolean sky = level.getDimension() == CloudLevel.DIMENSION_OVERWORLD;
            LightStateCache states = new LightStateCache(level.getServer().getBlockRegistry());
            LongFunction<LockedChunk> views = views(locked);
            int minHeight = level.getMinHeight();
            int maxHeight = level.getMaxHeight();
            LightPropagation block = new LightPropagation(LightChannel.BLOCK);
            ChunkLightAccess blockAccess = new ChunkLightAccess(LightChannel.BLOCK, minHeight, maxHeight, states, views);
            LightPropagation skyPropagation = new LightPropagation(LightChannel.SKY);
            ChunkLightAccess skyAccess = new ChunkLightAccess(LightChannel.SKY, minHeight, maxHeight, states, views);
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            for (int y = minHeight; y < maxHeight; y++) {
                for (int i = 0; i < 16; i++) {
                    invalidateIfDim(blockAccess, block, minX - 1, y, minZ + i);
                    invalidateIfDim(blockAccess, block, minX + 16, y, minZ + i);
                    invalidateIfDim(blockAccess, block, minX + i, y, minZ - 1);
                    invalidateIfDim(blockAccess, block, minX + i, y, minZ + 16);
                    if (sky) {
                        invalidateIfDim(skyAccess, skyPropagation, minX - 1, y, minZ + i);
                        invalidateIfDim(skyAccess, skyPropagation, minX + 16, y, minZ + i);
                        invalidateIfDim(skyAccess, skyPropagation, minX + i, y, minZ - 1);
                        invalidateIfDim(skyAccess, skyPropagation, minX + i, y, minZ + 16);
                    }
                }
            }
            block.run(blockAccess, Integer.MAX_VALUE);
            if (sky) {
                skyPropagation.run(skyAccess, Integer.MAX_VALUE);
            }
        } finally {
            for (int i = locked.length - 1; i >= 0; i--) {
                locked[i].close();
            }
        }
    }

    public static void update(CloudLevel level, LongCollection changed) {
        if (changed.isEmpty()) {
            return;
        }
        Set<CloudChunk> surrounding = new TreeSet<>();
        for (long packed : changed) {
            int chunkX = LightPositions.x(packed) >> 4;
            int chunkZ = LightPositions.z(packed) >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    CloudChunk neighbour = level.getLoadedChunk(chunkX + dx, chunkZ + dz);
                    if (neighbour != null) {
                        surrounding.add(neighbour);
                    }
                }
            }
        }
        if (surrounding.isEmpty()) {
            return;
        }
        LockedChunk[] locked = lockSorted(surrounding);
        try {
            boolean sky = level.getDimension() == CloudLevel.DIMENSION_OVERWORLD;
            LightStateCache states = new LightStateCache(level.getServer().getBlockRegistry());
            LongFunction<LockedChunk> views = views(locked);
            int minHeight = level.getMinHeight();
            int maxHeight = level.getMaxHeight();
            LightPropagation block = new LightPropagation(LightChannel.BLOCK);
            ChunkLightAccess blockAccess = new ChunkLightAccess(LightChannel.BLOCK, minHeight, maxHeight, states, views);
            LightPropagation skyPropagation = new LightPropagation(LightChannel.SKY);
            ChunkLightAccess skyAccess = new ChunkLightAccess(LightChannel.SKY, minHeight, maxHeight, states, views);
            for (long packed : changed) {
                block.invalidate(blockAccess, packed);
                if (sky) {
                    skyPropagation.invalidate(skyAccess, packed);
                }
            }
            block.run(blockAccess, Integer.MAX_VALUE);
            if (sky) {
                skyPropagation.run(skyAccess, Integer.MAX_VALUE);
            }
        } finally {
            for (int i = locked.length - 1; i >= 0; i--) {
                locked[i].close();
            }
        }
    }

    private static LockedChunk[] lockSorted(Collection<CloudChunk> chunks) {
        return chunks.stream().sorted().map(CloudChunk::lockForWrite).toArray(LockedChunk[]::new);
    }

    private static LongFunction<LockedChunk> views(LockedChunk[] locked) {
        Long2ObjectOpenHashMap<LockedChunk> byKey = new Long2ObjectOpenHashMap<>(locked.length);
        for (LockedChunk view : locked) {
            byKey.put(CloudChunk.key(view.getX(), view.getZ()), view);
        }
        return byKey::get;
    }

    private static void invalidateIfDim(ChunkLightAccess access, LightPropagation propagation, int x, int y, int z) {
        if (access.isLoaded(x, z)) {
            int light = access.light(x, y, z);
            if (light >= 1 && light <= 14) {
                propagation.invalidate(access, LightPositions.pack(x, y, z));
            }
        }
    }
}
