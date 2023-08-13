package org.cloudburstmc.server.level.manager;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.chunk.LockableChunk;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.generator.Generator;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

/**
 * Delegates chunk population to a {@link Generator}.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PopulationTask implements BiFunction<CloudChunk, List<CloudChunk>, CloudChunk> {
    public static final PopulationTask INSTANCE = new PopulationTask();

    @Override
    public CloudChunk apply(@NonNull CloudChunk chunk, List<CloudChunk> chunks) {
        if (chunk.isPopulated()) {
            return chunk;
        }
        Preconditions.checkState(chunk.isGenerated(), "Chunk %s,%s was populated before being generated!", chunk.getX(), chunk.getZ());

        RandomGenerator random = new FastPRandom(chunk.getX() * 6169336838570288771L ^ chunk.getZ() * 1173358236373774883L ^ chunk.getLevel().getSeed());

        chunks.add(chunk);
        LockableChunk[] lockableChunks = chunks.stream()
                .peek(populationChunk -> Preconditions.checkState(populationChunk.isGenerated(), "Chunk %d,%d was used for population before being generated!", populationChunk.getX(), populationChunk.getZ()))
                .map(Chunk::writeLockable)
                .sorted()
                .peek(Lock::lock)
                .toArray(LockableChunk[]::new);
        try {
            ((CloudLevel) chunk.getLevel()).getGenerator().populate(random, new PopulationChunkManager(chunk, lockableChunks, chunk.getLevel().getSeed()), chunk.getX(), chunk.getZ());
            chunk.setState(Chunk.STATE_POPULATED);
            chunk.setDirty();
        } finally {
            for (LockableChunk lockableChunk : lockableChunks) {
                lockableChunk.unlock();
            }
        }
        return chunk;
    }
}
