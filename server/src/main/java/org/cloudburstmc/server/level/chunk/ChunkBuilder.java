package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.level.BlockUpdate;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.ArrayList;
import java.util.List;

public class ChunkBuilder {

    private final int x;
    private final int z;
    private final CloudLevel level;
    private final List<BlockUpdate> blockUpdates = new ArrayList<>();
    private final List<ChunkDataLoader> chunkDataLoaders = new ArrayList<>();
    private CloudChunkSection[] sections;
    private int[] heightMap;
    private boolean dirty;
    private int state = Chunk.STATE_NEW;

    public ChunkBuilder(int x, int z, CloudLevel level) {
        this.x = x;
        this.z = z;
        this.level = Preconditions.checkNotNull(level, "level");
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public CloudLevel getLevel() {
        return level;
    }

    public ChunkBuilder sections(CloudChunkSection[] sections) {
        this.sections = Preconditions.checkNotNull(sections, "sections");
        return this;
    }

    public ChunkBuilder heightMap(int[] heightMap) {
        this.heightMap = Preconditions.checkNotNull(heightMap, "heightMap");
        return this;
    }

    public ChunkBuilder blockUpdate(BlockUpdate blockUpdate) {
        Preconditions.checkNotNull(blockUpdate, "blockUpdate");
        this.blockUpdates.add(blockUpdate);
        return this;
    }

    public ChunkBuilder dataLoader(ChunkDataLoader chunkDataLoader) {
        Preconditions.checkNotNull(chunkDataLoader, "chunkDataLoader");
        this.chunkDataLoaders.add(chunkDataLoader);
        return this;
    }

    public ChunkBuilder state(int state) {
        this.state = state;
        return this;
    }

    public ChunkBuilder dirty() {
        this.dirty = true;
        return this;
    }

    public CloudChunk build() {
        Preconditions.checkNotNull(this.sections, "sections");
        Preconditions.checkNotNull(this.heightMap, "heightMap");
        CloudChunk chunk = new CloudChunk(new UnsafeChunk(this.x, this.z, this.level, this.sections,
                this.heightMap), this.chunkDataLoaders, this.blockUpdates);
        if (this.state != Chunk.STATE_NEW) {
            chunk.setState(this.state);
        }
        chunk.setDirty(this.dirty);
        return chunk;
    }

    public interface Factory {

        ChunkBuilder create(int x, int z);
    }
}
