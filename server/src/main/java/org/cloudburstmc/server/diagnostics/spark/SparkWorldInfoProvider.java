package org.cloudburstmc.server.diagnostics.spark;

import lombok.extern.log4j.Log4j2;
import me.lucko.spark.common.platform.world.ChunkInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Publishes world counts for background sampling and confines detailed queries to the server thread.
 * Shutdown retains the last counts and makes detailed queries unavailable.
 */
@Log4j2
public class SparkWorldInfoProvider implements WorldInfoProvider, AutoCloseable {

    private final Thread owner = Thread.currentThread();
    private final WorldInfoProvider source;
    private final SparkMainThreadExecutor executor;
    private volatile CountsResult counts = new CountsResult(0, 0, 0, 0);
    private volatile boolean closed;

    public SparkWorldInfoProvider(WorldInfoProvider source, SparkMainThreadExecutor executor) {
        this.source = Objects.requireNonNull(source, "source");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Refreshes the published counts on the server thread before monitoring starts and during ticks.
     */
    public void refresh() {
        if (Thread.currentThread() != this.owner) {
            throw new IllegalStateException("Spark world counts must be captured on the server thread");
        }

        if (!this.closed) {
            this.counts = this.source.pollCounts();
        }
    }

    @Override
    public boolean mustCallSync() {
        return false;
    }

    @Override
    public CountsResult pollCounts() {
        return this.counts;
    }

    @Override
    public ChunksResult<? extends ChunkInfo<?>> pollChunks() {
        return this.poll(this.source::pollChunks);
    }

    @Override
    public GameRulesResult pollGameRules() {
        return this.poll(this.source::pollGameRules);
    }

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return this.poll(this.source::pollDataPacks);
    }

    /**
     * Stops accessing live worlds without discarding the last published counts.
     * Call before closing the server-thread executor or unloading worlds.
     */
    @Override
    public void close() {
        this.closed = true;
    }

    private <T> T poll(Supplier<T> query) {
        if (this.closed) {
            return null;
        }

        try {
            return CompletableFuture.supplyAsync(() -> this.closed ? null : query.get(), this.executor)
                    .get(5, TimeUnit.SECONDS);
        } catch (RejectedExecutionException failure) {
            if (this.closed) {
                return null;
            }
            throw failure;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        } catch (TimeoutException timeout) {
            log.warn("Timed out collecting Spark world details");
            return null;
        } catch (ExecutionException failure) {
            throw new IllegalStateException("Unable to collect Spark world details", failure.getCause());
        }
    }
}
