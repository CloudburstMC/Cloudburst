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
 * Delegates chunk population to a {@link Generator}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PopulationTask implements BiFunction<CloudChunk, List<CloudChunk>, CloudChunk> {
    public static final PopulationTask INSTANCE = new PopulationTask();

    @Override
    public CloudChunk apply(@NonNull CloudChunk chunk, List<CloudChunk> neighbors) {
        if (chunk.isPopulated()) {
            return chunk;
        }
        Preconditions.checkState(chunk.isGenerated(),
                "Chunk %s,%s was populated before being generated!",
                chunk.getX(),
                chunk.getZ());

        RandomGenerator random = new FastPRandom(chunk.getX() * 6169336838570288771L ^ chunk.getZ() * 1173358236373774883L ^ chunk.getLevel().getSeed());
        List<CloudChunk> chunks = new ArrayList<>(neighbors.size() + 1);
        chunks.addAll(neighbors);
        chunks.add(chunk);

        LockedChunk[] lockedChunks = chunks.stream()
                .peek(populationChunk -> Preconditions.checkState(populationChunk.isGenerated(), "Chunk %d,%d was used for population before being generated!", populationChunk.getX(), populationChunk.getZ()))
                .sorted()
                .map(CloudChunk::lockForWrite)
                .toArray(LockedChunk[]::new);

        try {
            ((CloudLevel) chunk.getLevel()).getGenerator().populate(random, new PopulationChunkManager(chunk, lockedChunks, chunk.getLevel().getSeed()), chunk.getX(), chunk.getZ());
            chunk.advanceGenerationStatus(ChunkGenerationStatus.POPULATED);
        } finally {
            for (int i = lockedChunks.length - 1; i >= 0; i--) {
                lockedChunks[i].close();
            }
        }

        return chunk;
    }
}
