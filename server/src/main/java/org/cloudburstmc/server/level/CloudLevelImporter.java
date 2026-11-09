package org.cloudburstmc.server.level;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.inject.LevelModule;
import org.cloudburstmc.server.level.provider.LevelImportFormat;
import org.cloudburstmc.server.level.provider.LevelImportSource;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBProvider;
import org.cloudburstmc.server.level.provider.migration.LevelDBImportProfile;
import org.cloudburstmc.server.level.provider.migration.LevelDBImportSource;
import org.cloudburstmc.server.level.provider.migration.StagedLevelImport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Converts and publishes offline worlds before their destination levels are registered.
 */
@UtilityClass
@Log4j2
public class CloudLevelImporter {

    /**
     * Converts and publishes one offline level, retaining its original directory as a backup.
     * The source directory must match the request ID and be directly inside {@code levelsPath}.
     */
    public static void importLevel(CloudServer server, Path levelsPath, CloudLevelLoadRequest request,
                                  LevelImportFormat format, Path sourcePath, Executor executor) throws IOException {
        long started = System.nanoTime();
        log.info("Converting world '{}' from {}", request.id(), format.getDisplayName());
        int chunks;
        Path backup;
        try (StagedLevelImport staging = new StagedLevelImport(levelsPath, request.id(), sourcePath)) {
            try (LevelImportSource source = format.open(sourcePath, request.dimension(), request.importProfile(), Runnable::run)) {
                chunks = convertDimension(server, request, source, staging.stagingRoot(), executor, false);
            }

            backup = staging.publish();
        }

        logCompletion(request.id(), Map.of(request.id(), chunks), backup, started);
    }

    /**
     * Converts the primary Overworld and its configured dimensions as one staged import.
     * The first request identifies the source Overworld. Other requests select distinct
     * dimensions and destination directories that must not already exist.
     * Dimensions with no saved chunks are initialized for generation with the source seed
     * and the configured generator. The complete source directory is retained as a backup.
     */
    public static void importDimensions(CloudServer server, Path levelsPath, List<CloudLevelLoadRequest> requests, Path sourcePath, Executor executor) throws IOException {
        if (requests.isEmpty() || requests.getFirst().dimension() != CloudLevel.DIMENSION_OVERWORLD
                || requests.stream().map(CloudLevelLoadRequest::dimension).distinct().count() != requests.size()) {
            throw new IllegalArgumentException("Combined import requires an Overworld followed by distinct dimensions");
        }

        long started = System.nanoTime();
        Map<String, Integer> chunks = new LinkedHashMap<>();
        Path backup;
        try (StagedLevelImport staging = new StagedLevelImport(levelsPath, requests.stream().map(CloudLevelLoadRequest::id).toList(), sourcePath)) {
            log.info("Importing world '{}'", requests.getFirst().id());
            try (LevelDBImportSource world = new LevelDBImportSource(sourcePath, CloudLevel.DIMENSION_OVERWORLD, LevelDBImportProfile.BEDROCK, Runnable::run)) {
                Set<Integer> storedDimensions = world.storedDimensions();
                for (CloudLevelLoadRequest request : requests) {
                    try (LevelDBImportSource source = world.forDimension(request.dimension())) {
                        boolean emptyDimension = !storedDimensions.contains(request.dimension());
                        if (emptyDimension) {
                            log.info("Preparing world '{}' ({}, no saved chunks)", request.id(), dimensionName(request.dimension()));
                        } else {
                            log.info("Converting world '{}' ({})", request.id(), dimensionName(request.dimension()));
                        }

                        int count = convertDimension(server, request, source, staging.stagingRoot(), executor, emptyDimension);
                        chunks.put(request.id(), count);
                    }
                }
            }

            backup = staging.publish();
        }

        logCompletion(requests.getFirst().id(), chunks, backup, started);
    }

    private static void logCompletion(String sourceId, Map<String, Integer> chunks, Path backup, long started) {
        chunks.forEach((id, count) -> {
            if (count == 0) {
                log.info("Prepared world '{}' for generation", id);
            } else {
                log.info("Converted world '{}' ({} chunks)", id, count);
            }
        });

        String elapsed = String.format(Locale.ROOT, "%.2f", (System.nanoTime() - started) / 1_000_000_000d);
        log.info("World import completed in {} s", elapsed);
        log.info("Original world '{}' retained as '{}'", sourceId, backup.getFileName());
    }

    private static String dimensionName(int dimension) {
        return switch (dimension) {
            case CloudLevel.DIMENSION_OVERWORLD -> "Overworld";
            case CloudLevel.DIMENSION_NETHER -> "Nether";
            case CloudLevel.DIMENSION_THE_END -> "End";
            default -> throw new IllegalArgumentException("Unknown dimension: " + dimension);
        };
    }

    private static int convertDimension(CloudServer server, CloudLevelLoadRequest request, LevelImportSource source,
                                       Path stagingRoot, Executor executor, boolean emptyDimension) throws IOException {
        CloudLevelData initial = request.createInitialData(server.getLevelDefaults());
        CloudLevelData data = source.loadLevelData(initial).join();
        data.setName(request.id());
        data.setDimension(request.dimension());
        data.setGenerator(request.generator());
        data.setGeneratorOptions(request.generatorOptions());
        if (request.dimension() != CloudLevel.DIMENSION_OVERWORLD) {
            data.setSpawn(initial.getSpawn());
        }

        try (LevelDBProvider destination = new LevelDBProvider(request.id(), stagingRoot, data, executor)) {
            int chunks = 0;
            if (!emptyDimension) {
                CloudLevel level = server.getInjector()
                        .createChildInjector(new LevelModule(request.id(), destination, data))
                        .getInstance(CloudLevel.class);
                chunks = new CloudLevelConverter(source, destination, level).convert().join();
            }

            destination.saveLevelData(data).join();
            return chunks;
        }
    }
}
