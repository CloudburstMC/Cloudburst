package org.cloudburstmc.server.player;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.server.PlayerDataSerializeEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.PlayerDataKey;
import org.cloudburstmc.api.util.PlayerDataSerializer;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.CloudServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@RequiredArgsConstructor
public final class CloudPlayerDataStore {
    private static final Pattern UUID_DATA_FILE = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.dat$", Pattern.CASE_INSENSITIVE);

    private final CloudServer server;

    public @Nullable NbtMap read(UUID uuid, boolean create) {
        return this.read(PlayerDataKey.of(uuid), create, true);
    }

    public @Nullable NbtMap read(String name, boolean create) {
        PlayerDataKey key = this.server.lookupName(name)
                .map(uuid -> PlayerDataKey.of(uuid, name))
                .orElseGet(() -> PlayerDataKey.named(name));
        return this.read(key, create, true);
    }

    public void save(UUID uuid, NbtMap tag, boolean async) {
        this.save(PlayerDataKey.of(uuid), tag, async, true);
    }

    public void save(String name, NbtMap tag, boolean async) {
        PlayerDataKey key = this.server.lookupName(name)
                .map(uuid -> PlayerDataKey.of(uuid, name))
                .orElseGet(() -> PlayerDataKey.named(name));
        this.save(key, tag, async, true);
    }

    public void convertLegacyData() {
        File dataDirectory = this.server.getDataPath().resolve("players").toFile();
        File[] files = dataDirectory.listFiles(file -> {
            String fileName = file.getName();
            Matcher matcher = UUID_DATA_FILE.matcher(fileName);
            return !matcher.matches() && fileName.endsWith(".dat");
        });

        if (files == null) {
            return;
        }

        for (File legacyData : files) {
            String name = legacyData.getName();
            name = name.substring(0, name.length() - 4);

            log.debug("Attempting legacy player data conversion for {}", name);

            NbtMap tag = this.read(PlayerDataKey.named(name), false, false);
            if (tag == null || !tag.containsKey("UUIDLeast") || !tag.containsKey("UUIDMost")) {
                continue;
            }

            UUID uuid = new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
            if (!tag.containsKey("NameTag")) {
                tag = tag.toBuilder().putString("NameTag", name).build();
            }

            if (this.server.getDataPath().resolve("players").resolve(uuid + ".dat").toFile().exists()) {
                continue;
            }

            this.save(PlayerDataKey.of(uuid, name), tag, false, false);
            this.server.updateName(uuid, name);

            if (!legacyData.delete()) {
                log.warn("Unable to delete legacy data for {}", name);
            }
        }
    }

    private @Nullable NbtMap read(PlayerDataKey key, boolean create, boolean fireEvent) {
        PlayerDataSerializeEvent event = new PlayerDataSerializeEvent(key, this.server.getPlayerDataSerializer());
        if (fireEvent) {
            this.server.getEventManager().fire(event);
        }

        Optional<InputStream> dataStream = Optional.empty();
        try {
            dataStream = event.getSerializer().read(event.getKey());
            if (dataStream.isPresent()) {
                try (NBTInputStream stream = NbtUtils.createGZIPReader(dataStream.get())) {
                    return (NbtMap) stream.readTag();
                }
            }
        } catch (IOException e) {
            log.warn(this.server.getLanguage().translate("cloudburst.data.playerCorrupted", key.getStorageId()));
            log.throwing(e);
        } finally {
            dataStream.ifPresent(stream -> {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.throwing(e);
                }
            });
        }

        if (!create) {
            return null;
        }

        log.info(this.server.getLanguage().translate("cloudburst.data.playerNotFound", key.getStorageId()));
        NbtMap tag = this.createDefaultData();
        this.save(key, tag, true, fireEvent);
        return tag;
    }

    private void save(PlayerDataKey key, NbtMap tag, boolean async, boolean fireEvent) {
        if (!this.server.shouldSavePlayerData()) {
            return;
        }

        PlayerDataSerializeEvent event = new PlayerDataSerializeEvent(key, this.server.getPlayerDataSerializer());
        if (fireEvent) {
            this.server.getEventManager().fire(event);
        }

        Runnable task = () -> this.write(event.getSerializer(), event.getKey(), tag);
        if (async) {
            this.server.getAsyncScheduler().runNow(null, ignored -> task.run());
        } else {
            task.run();
        }
    }

    private void write(PlayerDataSerializer serializer, PlayerDataKey key, NbtMap tag) {
        try (OutputStream dataStream = serializer.write(key);
             NBTOutputStream stream = NbtUtils.createGZIPWriter(dataStream)) {
            stream.writeTag(tag);
        } catch (Exception e) {
            log.error(this.server.getLanguage().translate("cloudburst.data.saveError", key.getStorageId(), e));
        }
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
