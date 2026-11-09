package org.cloudburstmc.server.level.provider.leveldb;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.level.provider.CloudLevelProviderContext;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.level.provider.LevelProviderFactory;
import org.cloudburstmc.server.level.provider.leveldb.serializer.LevelDBDataSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates LevelDB providers and detects existing storage without modifying its files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LevelDBProviderFactory implements LevelProviderFactory {

    public static final LevelDBProviderFactory INSTANCE = new LevelDBProviderFactory();

    @Override
    public LevelProvider create(CloudLevelProviderContext context) throws IOException {
        return new LevelDBProvider(context.request().id(), context.levelsPath(), context.initialData(), context.executor());
    }

    /**
     * Detects a database directory or a readable metadata file with the current native marker.
     * A database directory alone is sufficient and does not imply valid metadata or records.
     */
    @Override
    public boolean isCompatible(String levelId, Path levelsPath) {
        Path level = levelsPath.resolve(levelId);
        if (Files.isDirectory(level.resolve("db"))) {
            return true;
        }

        for (String name : new String[]{"level.dat", "level.dat_old"}) {
            try {
                NbtMap metadata = LevelDBDataSerializer.readMetadata(level.resolve(name));
                if (metadata.containsKey("CloudburstStorageVersion", NbtType.INT)
                        && metadata.getInt("CloudburstStorageVersion") == LevelDBDataSerializer.CLOUDBURST_STORAGE_VERSION) {
                    return true;
                }
            } catch (IOException ignored) {
                // Detection must not repair metadata or create a database.
            }
        }

        return false;
    }
}
