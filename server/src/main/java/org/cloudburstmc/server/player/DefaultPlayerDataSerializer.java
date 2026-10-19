package org.cloudburstmc.server.player;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.PlayerDataKey;
import org.cloudburstmc.api.util.PlayerDataSerializer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.persistence.NbtFiles;
import org.cloudburstmc.server.persistence.RecoverableFiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor
public class DefaultPlayerDataSerializer implements PlayerDataSerializer {
    private final Path directory;

    @Override
    public Optional<NbtMap> read(PlayerDataKey key) throws IOException {
        return RecoverableFiles.read(this.pathFor(key), NbtFiles::readCompressed);
    }

    @Override
    public void write(PlayerDataKey key, NbtMap data) throws IOException {
        RecoverableFiles.write(this.pathFor(key), NbtFiles.encodeCompressed(data), NbtFiles::readCompressed);
    }

    private Path pathFor(PlayerDataKey key) {
        Preconditions.checkNotNull(key, "key");
        return this.directory.resolve(key.getStorageId() + ".dat");
    }
}
