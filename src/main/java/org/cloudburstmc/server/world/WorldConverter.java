package org.cloudburstmc.server.world;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.world.chunk.ChunkBuilder;
import org.cloudburstmc.server.world.generator.impl.VoidGenerator;
import org.cloudburstmc.server.world.provider.WorldProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class WorldConverter {
    private final WorldProvider oldWorldProvider;
    private final WorldProvider newWorldProvider;
    private final World fakeWorld;

    public WorldConverter(WorldProvider oldWorldProvider, WorldProvider newWorldProvider) {
        this.oldWorldProvider = oldWorldProvider;
        this.newWorldProvider = newWorldProvider;

        WorldData data = new WorldData();
        data.setName("converting");
        data.setGenerator(VoidGenerator.ID);
        data.setRandomSeed(0L);
        this.fakeWorld = new World(Server.getInstance(), "converting", newWorldProvider, data);
    }

    public CompletableFuture<Void> perform() {
        ChunkBuilder.Factory factory = (x, z) -> new ChunkBuilder(x, z, fakeWorld);
        AtomicInteger converted = new AtomicInteger();
        return this.oldWorldProvider.forEachChunk(factory, (chunk, throwable) -> {
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
                this.newWorldProvider.saveChunk(chunk).join();
            } else {
                log.warn("Null chunk");
            }
        }).thenApply(aVoid -> {
            fakeWorld.close();
            return null;
        });
    }
}
