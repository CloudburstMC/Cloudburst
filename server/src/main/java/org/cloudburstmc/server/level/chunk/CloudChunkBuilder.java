package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects persisted chunk state before constructing a {@link CloudChunk}.
 */
public final class CloudChunkBuilder {
    @Getter
    private final int x;
    @Getter
    private final int z;
    @Getter
    private final CloudLevel level;

    private final List<CloudChunkLoadTask> loadTasks = new ArrayList<>();
    private CloudChunkSection[] sections;
    private int[] heightMap;
    private ChunkGenerationStatus generationStatus = ChunkGenerationStatus.NEW;
    @Getter
    private int storageVersion = -1;
    private boolean dirty;

    public CloudChunkBuilder(int x, int z, CloudLevel level) {
        this.x = x;
        this.z = z;
        this.level = Preconditions.checkNotNull(level, "level");
    }

    public void setSections(CloudChunkSection[] sections) {
        this.sections = Preconditions.checkNotNull(sections, "sections");
    }

    public void setHeightMap(int[] heightMap) {
        this.heightMap = Preconditions.checkNotNull(heightMap, "heightMap");
    }

    public void setGenerationStatus(ChunkGenerationStatus generationStatus) {
        this.generationStatus = Preconditions.checkNotNull(generationStatus, "generationStatus");
    }

    public void setStorageVersion(int storageVersion) {
        this.storageVersion = storageVersion;
    }

    public void addLoadTask(CloudChunkLoadTask loadTask) {
        this.loadTasks.add(Preconditions.checkNotNull(loadTask, "loadTask"));
    }

    public void markDirty() {
        this.dirty = true;
    }

    public CloudChunk build() {
        Preconditions.checkState(this.sections != null, "sections were not provided");
        Preconditions.checkState(this.heightMap != null, "height map was not provided");

        CloudChunk chunk = new CloudChunk(
                new UnsafeChunk(this.x, this.z, this.level, this.sections, this.heightMap, this.generationStatus),
                this.loadTasks
        );

        if (this.dirty) {
            chunk.markDirty();
        }

        return chunk;
    }
}
