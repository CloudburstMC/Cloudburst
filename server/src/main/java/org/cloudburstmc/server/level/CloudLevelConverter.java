package org.cloudburstmc.server.level;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.provider.LevelImportSource;
import org.cloudburstmc.server.level.provider.LevelProvider;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Converts every chunk from an iterable source provider into a target provider.
 */
@Log4j2
public class CloudLevelConverter {
    private final LevelImportSource source;
    private final LevelProvider target;
    private final CloudLevel level;

    public CloudLevelConverter(LevelImportSource source, LevelProvider target, CloudLevel level) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        this.level = Objects.requireNonNull(level, "level");
    }

    public CompletableFuture<Void> convert() {
        ChunkBuilder.Factory factory = (x, z) -> new ChunkBuilder(x, z, this.level);
        AtomicInteger converted = new AtomicInteger();
        return this.source.visitChunks(factory, chunk -> {
            chunk.init();
            this.target.saveChunk(chunk).join();
            int count = converted.incrementAndGet();
            if ((count & 1023) == 512) {
                log.info("{} chunks converted", count);
            }
        }).thenRun(() -> log.info("Conversion completed. {} chunks converted", converted.get()));
    }
}
