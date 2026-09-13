package org.cloudburstmc.server.level.provider.leveldb;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.level.provider.CloudLevelProviderContext;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.level.provider.LevelProviderFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LevelDBProviderFactory implements LevelProviderFactory {

    public static final LevelDBProviderFactory INSTANCE = new LevelDBProviderFactory();

    @Override
    public LevelProvider create(CloudLevelProviderContext context) throws IOException {
        return new LevelDBProvider(context.request().id(), context.levelsPath(), context.executor());
    }

    @Override
    public boolean isCompatible(String levelId, Path levelsPath) {
        Path dbPath = levelsPath.resolve(levelId).resolve("db");
        if (!Files.isDirectory(dbPath)) {
            return false;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(dbPath, "*.ldb")) {
            for (Path file : files) {
                if (Files.isRegularFile(file)) {
                    return true;
                }
            }
        } catch (IOException exception) {
            return false;
        }

        return false;
    }
}
