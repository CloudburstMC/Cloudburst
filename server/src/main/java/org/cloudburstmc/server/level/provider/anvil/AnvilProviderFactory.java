package org.cloudburstmc.server.level.provider.anvil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.inject.LevelModule;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.CloudLevelConverter;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.provider.CloudLevelProviderContext;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.level.provider.LevelProviderFactory;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBProviderFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnvilProviderFactory implements LevelProviderFactory {

    public static final AnvilProviderFactory INSTANCE = new AnvilProviderFactory();

    @Override
    public LevelProvider create(CloudLevelProviderContext context) throws IOException {
        String levelId = context.request().id();
        Path levelsPath = context.levelsPath();
        try {
            try (AnvilLevelImportSource source = new AnvilLevelImportSource(levelId, levelsPath, context.executor());
                 LevelProvider target = LevelDBProviderFactory.INSTANCE.create(context)) {
                CloudLevelData data = source.loadLevelData(context.initialData()).join().orElse(context.initialData());
                data.setDimension(context.request().dimension());
                CloudLevel conversionLevel = context.server().getInjector()
                        .createChildInjector(new LevelModule(levelId, target, data))
                        .getInstance(CloudLevel.class);

                new CloudLevelConverter(source, target, conversionLevel).convert().join();
                target.saveLevelData(data).join();
            }
        } catch (CompletionException exception) {
            throw new IOException("Could not import level '" + levelId + "'", exception.getCause());
        }

        deleteRegionDirectory(levelsPath.resolve(levelId).resolve("region"));

        return LevelDBProviderFactory.INSTANCE.create(context);
    }

    @Override
    public boolean isCompatible(String levelId, Path levelsPath) {
        Path regionPath = levelsPath.resolve(levelId).resolve("region");
        if (!Files.isDirectory(regionPath)) {
            return false;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(regionPath, "*.mca")) {
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

    private static void deleteRegionDirectory(Path regionPath) throws IOException {
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(regionPath)) {
            paths = walk.sorted(Comparator.reverseOrder()).toList();
        }

        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
