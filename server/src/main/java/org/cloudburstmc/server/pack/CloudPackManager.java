package org.cloudburstmc.server.pack;

import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.pack.ResourcePack;
import org.cloudburstmc.api.registry.ResourcePackRegistry;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Log4j2
@Singleton
public class CloudPackManager implements ResourcePackRegistry {

    private final Map<UUID, CloudResourcePack> packs = new LinkedHashMap<>();
    private boolean registrationClosed;
    private boolean disposed;

    public synchronized void loadPacks(Path directory) {
        this.checkRegistrationOpen();

        try (Stream<Path> paths = Files.list(directory)) {
            for (Path source : paths.sorted().toList()) {
                String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS) && !name.endsWith(".zip") && !name.endsWith(".mcpack")) {
                    continue;
                }

                try {
                    this.loadPack(source);
                } catch (IOException | RuntimeException failure) {
                    log.warn("Unable to load resource pack {}", source, failure);
                }
            }
        } catch (IOException failure) {
            log.error("Unable to list resource packs in {}", directory, failure);
        }
    }

    public synchronized void loadPack(Path source) throws IOException {
        this.checkRegistrationOpen();

        CloudResourcePack pack = CloudPackLoader.load(source);
        if (this.packs.containsKey(pack.getId())) {
            pack.close();
            throw new IOException("A resource pack with UUID " + pack.getId() + " is already registered");
        }

        this.packs.put(pack.getId(), pack);
    }

    @Override
    public synchronized void close() {
        this.checkRegistrationOpen();
        boolean removed;
        do {
            removed = false;
            Iterator<CloudResourcePack> iterator = this.packs.values().iterator();
            while (iterator.hasNext()) {
                CloudResourcePack pack = iterator.next();
                for (CloudPackDependency dependency : pack.getDependencies()) {
                    CloudResourcePack required = this.packs.get(dependency.uuid());
                    if (required == null || !required.getVersion().toString().equals(dependency.version().toString())) {
                        log.warn("Resource pack {} requires unavailable pack {} version {}", pack.getId(), dependency.uuid(), dependency.version());
                        iterator.remove();
                        dispose(pack);
                        removed = true;
                        break;
                    }
                }
            }
        } while (removed);

        Map<UUID, CloudResourcePack> ordered = new LinkedHashMap<>();
        List<CloudResourcePack> pending = new ArrayList<>(this.packs.values());
        boolean progressed;
        do {
            progressed = false;
            Iterator<CloudResourcePack> iterator = pending.iterator();
            while (iterator.hasNext()) {
                CloudResourcePack pack = iterator.next();
                if (pack.getDependencies().stream().allMatch(dependency -> ordered.containsKey(dependency.uuid()))) {
                    ordered.put(pack.getId(), pack);
                    iterator.remove();
                    progressed = true;
                }
            }
        } while (progressed && !pending.isEmpty());

        for (CloudResourcePack pack : pending) {
            log.warn("Resource pack {} has a cyclic or unresolved dependency", pack.getId());
            dispose(pack);
        }

        this.packs.clear();
        this.packs.putAll(ordered);
        this.registrationClosed = true;
        log.info("Loaded {} resource packs", this.packs.size());
    }

    public synchronized ResourcePacksInfoPacket createInfoPacket(boolean required) {
        this.checkReady();
        ResourcePacksInfoPacket packet = new ResourcePacksInfoPacket();
        packet.setForcedToAccept(required);
        packet.setWorldTemplateId(new UUID(0, 0));
        packet.setWorldTemplateVersion("");

        for (CloudResourcePack pack : this.packs.values()) {
            packet.getResourcePackInfos().add(new ResourcePacksInfoPacket.Entry(
                    pack.getId(),
                    pack.getVersion().toString(),
                    pack.getSize(),
                    "",
                    "",
                    "",
                    false,
                    false,
                    false,
                    ""
            ));
        }

        return packet;
    }

    public synchronized ResourcePackStackPacket createStackPacket(boolean required) {
        this.checkReady();
        ResourcePackStackPacket packet = new ResourcePackStackPacket();
        packet.setForcedToAccept(required);
        packet.setGameVersion("*");

        for (CloudResourcePack pack : this.packs.values()) {
            packet.getResourcePacks().add(new ResourcePackStackPacket.Entry(
                    pack.getId().toString(),
                    pack.getVersion().toString(),
                    ""
            ));
        }

        return packet;
    }

    @Nullable
    public synchronized CloudResourcePack getPackByIdVersion(String idVersion) {
        this.checkReady();
        int separator = idVersion.lastIndexOf('_');
        if (separator < 0) {
            return null;
        }

        try {
            CloudResourcePack pack = this.packs.get(UUID.fromString(idVersion.substring(0, separator)));
            return pack != null && pack.getVersion().toString().equals(idVersion.substring(separator + 1)) ? pack : null;
        } catch (IllegalArgumentException failure) {
            return null;
        }
    }

    @Override
    public synchronized Optional<ResourcePack> get(UUID id) {
        return Optional.ofNullable(this.packs.get(Objects.requireNonNull(id, "id")));
    }

    @Override
    public synchronized Collection<ResourcePack> values() {
        return List.copyOf(this.packs.values());
    }

    public synchronized void shutdown() {
        if (this.disposed) {
            return;
        }

        this.disposed = true;
        for (CloudResourcePack pack : this.packs.values()) {
            dispose(pack);
        }

        this.packs.clear();
    }

    private void checkRegistrationOpen() {
        if (this.registrationClosed || this.disposed) {
            throw new IllegalStateException("Resource pack registration is closed");
        }
    }

    private void checkReady() {
        if (!this.registrationClosed || this.disposed) {
            throw new IllegalStateException("Resource packs are not available for transfer");
        }
    }

    private void dispose(CloudResourcePack pack) {
        try {
            pack.close();
        } catch (IOException failure) {
            log.error("Unable to release resource pack {}", pack.getId(), failure);
        }
    }
}
