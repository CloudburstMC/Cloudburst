package org.cloudburstmc.server.level.manager;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.level.generator.Generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

/**
 * Delegates chunk finishing to a {@link Generator}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FinishingTask implements BiFunction<CloudChunk, List<CloudChunk>, CloudChunk> {
    public static final FinishingTask INSTANCE = new FinishingTask();

    @Override
    public CloudChunk apply(@NonNull CloudChunk chunk, List<CloudChunk> neighbors) {
        if (chunk.isFinished()) {
            return chunk;
        }
        Preconditions.checkState(chunk.isPopulated(), "Chunk %s,%s was finished before being populated!", chunk.getX(), chunk.getZ());

        RandomGenerator random = new FastPRandom(chunk.getX() * 9050650275199519859L ^ chunk.getZ() * 5251710924988638743L ^ chunk.getLevel().getSeed());
        List<CloudChunk> chunks = new ArrayList<>(neighbors.size() + 1);
        chunks.addAll(neighbors);
        chunks.add(chunk);

        LockedChunk[] lockedChunks = chunks.stream()
                .peek(populationChunk -> Preconditions.checkState(populationChunk.isPopulated(), "Chunk %d,%d was used for finishing before being populated!", populationChunk.getX(), populationChunk.getZ()))
                .sorted()
                .map(CloudChunk::lockForWrite)
                .toArray(LockedChunk[]::new);

        try {
            ((CloudLevel) chunk.getLevel()).getGenerator().finish(random, new PopulationChunkManager(chunk, lockedChunks, chunk.getLevel().getSeed()), chunk.getX(), chunk.getZ());
            chunk.advanceGenerationStatus(ChunkGenerationStatus.FINISHED);
        } finally {
            for (int i = lockedChunks.length - 1; i >= 0; i--) {
                lockedChunks[i].close();
            }
        }

        return chunk;
    }
}
