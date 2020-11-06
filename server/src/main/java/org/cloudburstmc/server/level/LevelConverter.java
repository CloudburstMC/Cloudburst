package org.cloudburstmc.server.level;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.generator.impl.VoidGenerator;
import org.cloudburstmc.server.level.provider.LevelProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class LevelConverter {
    private final LevelProvider oldLevelProvider;
    private final LevelProvider newLevelProvider;
    private final Level fakeLevel;

    public LevelConverter(LevelProvider oldLevelProvider, LevelProvider newLevelProvider) {
        this.oldLevelProvider = oldLevelProvider;
        this.newLevelProvider = newLevelProvider;

        LevelData data = new LevelData();
        data.setName("converting");
        data.setGenerator(VoidGenerator.ID);
        data.setRandomSeed(0L);
        this.fakeLevel = new Level(CloudServer.getInstance(), "converting", newLevelProvider, data);
    }

    public CompletableFuture<Void> perform() {
        ChunkBuilder.Factory factory = (x, z) -> new ChunkBuilder(x, z, fakeLevel);
        AtomicInteger converted = new AtomicInteger();
        return this.oldLevelProvider.forEachChunk(factory, (chunk, throwable) -> {
            if (throwable != null) {
                log.error("Unable to convert chunk", throwable);
                return;
            }

            if (chunk != null) {
                int count = converted.incrementAndGet();
                if ((count & 1023) == 512) {
                    log.info("{} chunks converted", count);
                }
                chunk.init();
                this.newLevelProvider.saveChunk(chunk).join();
            } else {
                log.warn("Null chunk");
            }
        }).thenApply(aVoid -> {
            fakeLevel.close();
            return null;
        });
    }
}
