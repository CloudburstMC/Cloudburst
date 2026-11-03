package org.cloudburstmc.server.level.light.oracle;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongFunction;

public class OracleChunkLightAccess implements OracleLightAccess {

    private final BlockRegistry registry;
    private final int minHeight;
    private final int maxHeight;
    private final Type type;
    private final LongFunction<Chunk> loadedChunks;
    private final Map<Long, @Nullable Chunk> chunks = new HashMap<>();

    public OracleChunkLightAccess(BlockRegistry registry, int minHeight, int maxHeight, Type type, LongFunction<Chunk> loadedChunks) {
        this.registry = registry;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.type = type;
        this.loadedChunks = loadedChunks;
    }

    @Override
    public BlockRegistry registry() {
        return this.registry;
    }

    @Override
    public int minHeight() {
        return this.minHeight;
    }

    @Override
    public int maxHeight() {
        return this.maxHeight;
    }

    @Override
    public @Nullable Sample read(Vector3i position) {
        if (position.getY() < this.minHeight() || position.getY() >= this.maxHeight()) {
            return null;
        }

        Chunk chunk = this.chunk(position);

        if (chunk == null) {
            return null;
        }

        int x = position.getX() & 15;
        int y = position.getY();
        int z = position.getZ() & 15;
        int light = this.type == Type.BLOCK ? chunk.getBlockLight(x, y, z) : chunk.getSkyLight(x, y, z);

        return new Sample(chunk.getBlockState(x, y, z, BlockLayer.PRIMARY), chunk.getBlockState(x, y, z, BlockLayer.SECONDARY), light);
    }

    @Override
    public void write(Vector3i position, int light) {
        if (position.getY() < this.minHeight() || position.getY() >= this.maxHeight()) {
            return;
        }

        Chunk chunk = this.chunk(position);

        if (chunk != null) {
            if (this.type == Type.BLOCK) {
                chunk.setBlockLight(position.getX() & 15, position.getY(), position.getZ() & 15, light);
            } else {
                chunk.setSkyLight(position.getX() & 15, position.getY(), position.getZ() & 15, light);
            }
        }
    }

    private @Nullable Chunk chunk(Vector3i position) {
        int x = position.getX() >> 4;
        int z = position.getZ() >> 4;

        long key = CloudChunk.key(x, z);

        if (!this.chunks.containsKey(key)) {
            this.chunks.put(key, this.loadedChunks.apply(key));
        }

        return this.chunks.get(key);
    }
}
