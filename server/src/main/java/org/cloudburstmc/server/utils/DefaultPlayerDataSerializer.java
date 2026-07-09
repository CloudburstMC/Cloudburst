package org.cloudburstmc.server.utils;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.PlayerDataKey;
import org.cloudburstmc.api.util.PlayerDataSerializer;
import org.cloudburstmc.server.CloudServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor
public class DefaultPlayerDataSerializer implements PlayerDataSerializer {
    private final CloudServer server;

    @Override
    public Optional<InputStream> read(PlayerDataKey key) throws IOException {
        Path path = this.pathFor(key);
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        return Optional.of(Files.newInputStream(path));
    }

    @Override
    public OutputStream write(PlayerDataKey key) throws IOException {
        Path path = this.pathFor(key);
        return new AtomicFileOutputStream(path);
    }

    private Path pathFor(PlayerDataKey key) {
        Preconditions.checkNotNull(key, "key");
        return this.server.getDataPath().resolve("players").resolve(key.getStorageId() + ".dat");
    }
}
