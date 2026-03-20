package org.cloudburstmc.server.level;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.level.LevelLoadEvent;
import org.cloudburstmc.api.event.level.LevelUnloadEvent;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.utils.Utils;

import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Singleton
public class LevelManager implements Closeable {

    /**
     * Virtual-thread executor for LevelDB reads and writes.
     */
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Bounded pool for CPU-bound chunk generation, population, and finishing.
     */
    private final ForkJoinPool generationExecutor = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            new GenerationThreadFactory(),
            (thread, ex) -> log.error("Uncaught exception on generation thread {}", thread.getName(), ex),
            false
    );

    private final CloudServer server;
    private final Set<CloudLevel> levels = new HashSet<>();
    private final Map<String, CloudLevel> levelIds = new HashMap<>();
    private volatile CloudLevel defaultLevel;

    @Inject
    public LevelManager(CloudServer server) {
        this.server = server;
    }

    public synchronized void register(CloudLevel level) {
        Preconditions.checkNotNull(level, "level");
        Preconditions.checkArgument(level.getServer() == this.server, "Level did not come from this server");
        Preconditions.checkArgument(!levels.contains(level), "level already registered");

        LevelLoadEvent event = new LevelLoadEvent(level);
        this.server.getEventManager().fire(event);

        this.levels.add(level);
        this.levelIds.put(level.getId(), level);
    }

    public boolean deregister(CloudLevel level) {
        return deregister(level, false);
    }

    public synchronized boolean deregister(CloudLevel level, boolean force) {
        Preconditions.checkNotNull(level, "level");
        Preconditions.checkArgument(levels.contains(level), "level not registered");

        LevelUnloadEvent event = new LevelUnloadEvent(level);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() && !force) {
            return false;
        } else {
            this.levelIds.remove(level.getId());
            return levels.remove(level);
        }
    }

    @Nullable
    public synchronized CloudLevel getLevel(String id) {
        return this.levelIds.get(id);
    }

    @Nullable
    public synchronized CloudLevel getLevelByName(String name) {
        for (CloudLevel level : this.levels) {
            if (level.getName().equals(name)) {
                return level;
            }
        }
        return null;
    }

    public CloudLevel getDefaultLevel() {
        return defaultLevel;
    }

    public synchronized void setDefaultLevel(CloudLevel level) {
        Preconditions.checkNotNull(level, "level");
        Preconditions.checkArgument(levels.contains(level), "level not registered");

        this.defaultLevel = level;
    }

    public synchronized Set<CloudLevel> getLevels() {
        return ImmutableSet.copyOf(levels);
    }

    public synchronized void save() {
        this.levels.forEach(CloudLevel::save);
    }

    @Override
    public void close() {
        synchronized (this) {
            for (CloudLevel level : this.levels) {
                try {
                    level.close();
                } catch (Exception e) {
                    log.error("Error closing level " + level.getId(), e);
                }
            }
        }

        this.ioExecutor.shutdown();
        try {
            if (!this.ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("I/O executor did not terminate in time, forcing shutdown");
                this.ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        this.generationExecutor.shutdown();
        try {
            if (!this.generationExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Generation executor did not terminate in time, forcing shutdown");
                this.generationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.generationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void tick(int currentTick) {
        for (CloudLevel level : this.levels) {
            try {
                long levelTime = System.currentTimeMillis();
                level.doTick(currentTick);
                int tickMs = (int) (System.currentTimeMillis() - levelTime);
                level.tickRateTime = tickMs;

                if (server.isAutoTickRate()) {
                    if (tickMs < 50 && level.getTickRate() > server.getBaseTickRate()) {
                        int r;
                        level.setTickRate(r = level.getTickRate() - 1);
                        if (r > server.getBaseTickRate()) {
                            level.tickRateCounter = level.getTickRate();
                        }
                        log.debug("Raising level \"" + level.getName() + "\" tick rate to " + level.getTickRate() + " ticks");
                    } else if (tickMs >= 50) {
                        if (level.getTickRate() == server.getBaseTickRate()) {
                            level.setTickRate(Math.max(server.getBaseTickRate() + 1, Math.min(server.getAutoTickRateLimit(), tickMs / 50)));
                            log.debug("Level \"" + level.getName() + "\" took " + GenericMath.round(tickMs, 2) + "ms, setting tick rate to " + level.getTickRate() + " ticks");
                        } else if ((tickMs / level.getTickRate()) >= 50 && level.getTickRate() < server.getAutoTickRateLimit()) {
                            level.setTickRate(level.getTickRate() + 1);
                            log.debug("Level \"" + level.getName() + "\" took " + GenericMath.round(tickMs, 2) + "ms, setting tick rate to " + level.getTickRate() + " ticks");
                        }
                        level.tickRateCounter = level.getTickRate();
                    }
                }

                if (currentTick % 100 == 0) {
                    level.doChunkGarbageCollection();
                }
            } catch (Exception e) {
                log.error(server.getLanguage().translate("cloudburst.level.tickError", level.getId(), Utils.getExceptionMessage(e)));
            }
        }
    }

    public ExecutorService getIoExecutor() {
        return ioExecutor;
    }

    /**
     * Returns the bounded pool used for CPU-bound chunk generation work.
     * Callers should submit generation, population, and finishing tasks here
     * rather than to a virtual-thread pool.
     */
    public ForkJoinPool getGenerationExecutor() {
        return generationExecutor;
    }

    /**
     * Names worker threads in the generation pool for easy identification
     * in thread dumps and profiler output.
     */
    private static final class GenerationThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName("Cloudburst Chunk Generation #" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
