package org.cloudburstmc.server.player;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.server.PlayerDataSerializeEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.PlayerDataKey;
import org.cloudburstmc.api.util.PlayerDataSerializer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.CloudServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@RequiredArgsConstructor
public class CloudPlayerDataStore implements AutoCloseable {

    private final CloudServer server;
    private final ThreadLocal<Boolean> saving = ThreadLocal.withInitial(() -> false);
    private final ExecutorService writer = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("Cloudburst Player Data Writer").factory());

    public @Nullable NbtMap read(UUID uuid, boolean create) {
        return this.read(PlayerDataKey.of(uuid), create);
    }

    public @Nullable NbtMap read(String name, boolean create) {
        Optional<UUID> uuid = this.server.lookupName(name);
        if (uuid.isEmpty()) {
            if (create) {
                throw new IllegalStateException("Player identity is unknown: " + name);
            }
            return null;
        }

        return this.read(PlayerDataKey.of(uuid.get(), name), create);
    }

    public void save(UUID uuid, NbtMap tag, boolean async) {
        this.save(PlayerDataKey.of(uuid), tag, async);
    }

    public void save(String name, NbtMap tag, boolean async) {
        PlayerDataKey key = this.server.lookupName(name)
                .map(uuid -> PlayerDataKey.of(uuid, name))
                .orElseThrow(() -> new IllegalStateException("Player identity is unknown: " + name));
        this.save(key, tag, async);
    }

    private @Nullable NbtMap read(PlayerDataKey key, boolean create) {
        requireExternalCaller();
        CompletableFuture.runAsync(() -> {}, writer).join();
        PlayerDataSerializeEvent event = new PlayerDataSerializeEvent(key, this.server.getPlayerDataSerializer());
        this.server.getEventManager().fire(event);

        try {
            Optional<NbtMap> data = event.getSerializer().read(event.getKey());
            if (data.isPresent()) {
                return data.get();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load player data for " + key.uniqueId(), e);
        }

        if (!create) {
            return null;
        }

        log.info(this.server.getLanguage().translate("cloudburst.data.playerNotFound", key.getStorageId()));
        NbtMap tag = this.createDefaultData();
        this.save(key, tag, true);
        return tag;
    }

    private void save(PlayerDataKey key, NbtMap tag, boolean async) {
        requireExternalCaller();
        if (!this.server.shouldSavePlayerData()) {
            return;
        }

        PlayerDataSerializeEvent event = new PlayerDataSerializeEvent(key, this.server.getPlayerDataSerializer());
        this.server.getEventManager().fire(event);

        CompletableFuture<Void> save = CompletableFuture.runAsync(() -> this.write(event.getSerializer(), event.getKey(), tag), writer);
        if (!async) {
            save.join();
        }
    }

    private void write(PlayerDataSerializer serializer, PlayerDataKey key, NbtMap tag) {
        saving.set(true);
        try {
            serializer.write(key, tag);
        } catch (Exception e) {
            log.error("Unable to save player data for {}", key.uniqueId(), e);
            throw new IllegalStateException("Unable to save player data for " + key.uniqueId(), e);
        } finally {
            saving.remove();
        }
    }

    private void requireExternalCaller() {
        if (saving.get()) {
            throw new IllegalStateException("Player-data storage callbacks must not re-enter player-data operations");
        }
    }

    @Override
    public void close() {
        requireExternalCaller();
        writer.close();
    }

    private NbtMap createDefaultData() {
        Location spawn = this.server.getDefaultLevel().getSafeSpawn();
        long now = System.currentTimeMillis() / 1000;
        return NbtMap.builder()
                .putLong("firstPlayed", now)
                .putLong("lastPlayed", now)
                .putList("Pos", NbtType.FLOAT, Arrays.asList(
                        spawn.getPosition().getX(),
                        spawn.getPosition().getY(),
                        spawn.getPosition().getZ()
                ))
                .putString("Level", this.server.getDefaultLevel().getName())
                .putInt("playerGameType", this.server.getGameMode().getVanillaId())
                .putList("Rotation", NbtType.FLOAT, Arrays.asList(
                        spawn.getYaw(),
                        spawn.getPitch()
                ))
                .build();
    }
}
