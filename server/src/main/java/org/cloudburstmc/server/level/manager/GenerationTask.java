package org.cloudburstmc.server.level.manager;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.level.generator.Generator;

import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * Delegates chunk generation to a {@link Generator}.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GenerationTask implements Function<CloudChunk, CloudChunk> {
    public static final GenerationTask INSTANCE = new GenerationTask();

    @Override
    public CloudChunk apply(@NonNull CloudChunk chunk) {
        if (chunk.isGenerated()) {
            return chunk;
        }

        RandomGenerator random = new FastPRandom(chunk.getX() * 3053330778986901431L ^ chunk.getZ() * 1517227374085824433L ^ chunk.getLevel().getSeed());
        try (LockedChunk locked = chunk.lockForWrite()) {
            ((CloudLevel) chunk.getLevel()).getGenerator().generate(random, locked, chunk.getX(), chunk.getZ());
            chunk.advanceGenerationStatus(ChunkGenerationStatus.GENERATED);
        }
        return chunk;
    }
}
