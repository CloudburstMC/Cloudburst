package org.cloudburstmc.server.level;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.inject.LevelModule;
import org.cloudburstmc.server.level.provider.LevelImportFormat;
import org.cloudburstmc.server.level.provider.LevelImportSource;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBProvider;
import org.cloudburstmc.server.level.provider.migration.StagedLevelImport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Converts an offline source without loading or registering the destination level.
 */
@UtilityClass
@Log4j2
public class CloudLevelImporter {

    public static void importLevel(CloudServer server, Path levelsPath, CloudLevelLoadRequest request,
                                  LevelImportFormat format, Path sourcePath, Executor executor) throws IOException {
        long started = System.nanoTime();
        log.info("Converting level '{}' from {}", request.id(), format.getDisplayName());
        int chunks;
        Path backup;
        try (StagedLevelImport staging = new StagedLevelImport(levelsPath, request.id(), sourcePath)) {
            try (LevelImportSource source = format.open(sourcePath, request.dimension(), Runnable::run);
                 LevelDBProvider destination = new LevelDBProvider(request.id(), staging.stagingRoot(), executor)) {
                CloudLevelData data = source.loadLevelData(request.createInitialData(server.getLevelDefaults())).join();
                data.setName(request.id());
                data.setDimension(request.dimension());
                data.setGenerator(request.generator());
                data.setGeneratorOptions(request.generatorOptions());

                CloudLevel level = server.getInjector()
                        .createChildInjector(new LevelModule(request.id(), destination, data))
                        .getInstance(CloudLevel.class);
                chunks = new CloudLevelConverter(source, destination, level).convert().join();
                if (chunks == 0) {
                    throw new IOException("Import source contains no chunks for dimension " + request.dimension());
                }

                destination.saveLevelData(data).join();
            }

            backup = staging.publish();
        }

        String elapsed = String.format(Locale.ROOT, "%.2f", (System.nanoTime() - started) / 1_000_000_000d);
        log.info("Converted level '{}' ({} chunks) in {} s", request.id(), chunks, elapsed);
        log.info("Original level '{}' retained as '{}'", request.id(), backup.getFileName());
    }
}
