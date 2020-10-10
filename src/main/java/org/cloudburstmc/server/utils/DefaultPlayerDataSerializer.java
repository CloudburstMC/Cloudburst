package org.cloudburstmc.server.utils;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.CloudServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class DefaultPlayerDataSerializer implements PlayerDataSerializer {
    private final CloudServer server;

    @Override
    public Optional<InputStream> read(String name, UUID uuid) throws IOException {
        Path path = server.getDataPath().resolve("players/" + name + ".dat");
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        return Optional.of(Files.newInputStream(path));

    }

    @Override
    public OutputStream write(String name, UUID uuid) throws IOException {
        Preconditions.checkNotNull(name, "name");
        Path path = server.getDataPath().resolve("players/" + name + ".dat");
        return Files.newOutputStream(path);
    }
}
