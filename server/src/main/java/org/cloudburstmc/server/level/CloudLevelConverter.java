package org.cloudburstmc.server.level;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilderFactory;
import org.cloudburstmc.server.level.provider.LevelImportSource;
import org.cloudburstmc.server.level.provider.LevelProvider;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Converts saved chunk records into a separate destination provider.
 */
@Log4j2
public class CloudLevelConverter {

    private static final long PROGRESS_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final LevelImportSource source;
    private final LevelProvider target;
    private final CloudLevel level;

    public CloudLevelConverter(LevelImportSource source, LevelProvider target, CloudLevel level) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        this.level = Objects.requireNonNull(level, "level");
    }

    public CompletableFuture<Integer> convert() {
        CloudChunkBuilderFactory factory = (x, z) -> new CloudChunkBuilder(x, z, this.level);
        AtomicInteger converted = new AtomicInteger();
        AtomicLong lastProgress = new AtomicLong(System.nanoTime());
        return this.source.visitChunks(factory, chunk -> {
            this.target.saveImportedChunk(chunk).join();
            int count = converted.incrementAndGet();
            long now = System.nanoTime();
            long previous = lastProgress.get();
            if (now - previous >= PROGRESS_INTERVAL_NANOS && lastProgress.compareAndSet(previous, now)) {
                log.info("Converting level '{}': {} chunks saved", this.level.getId(), count);
            }
        }).thenApply(ignored -> converted.get());
    }
}
