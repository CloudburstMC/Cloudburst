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
 * Metadata loading and destination publication are handled by the caller.
 */
@Log4j2
public class CloudLevelConverter {

    private static final long PROGRESS_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(15);

    private final LevelImportSource source;
    private final LevelProvider target;
    private final CloudLevel level;

    /**
     * Binds a source to its destination without starting conversion or taking ownership of either.
     *
     * @param source offline source whose selected chunks will be converted
     * @param target destination provider with no saved chunks at the source coordinates
     * @param level  destination level supplying chunk construction context
     */
    public CloudLevelConverter(LevelImportSource source, LevelProvider target, CloudLevel level) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        this.level = Objects.requireNonNull(level, "level");
    }

    /**
     * Saves the chunks selected by the source and finalizes entity chunk assignments.
     * The source and destination must remain open until the returned future completes.
     * Failure completes the future exceptionally and does not roll back chunks already saved.
     *
     * @return future completed with the number of converted terrain chunks
     */
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
                log.info("Converting world '{}': {} chunks saved", this.level.getId(), count);
            }
        }).thenCompose(ignored -> this.target.completeImport()).thenApply(ignored -> converted.get());
    }
}
