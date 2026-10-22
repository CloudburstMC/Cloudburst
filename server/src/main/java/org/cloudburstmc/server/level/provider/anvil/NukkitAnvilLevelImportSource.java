package org.cloudburstmc.server.level.provider.anvil;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilderFactory;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.cloudburstmc.server.level.provider.LevelImportSource;
import org.cloudburstmc.server.level.provider.migration.NukkitBlockStateConverter;
import org.cloudburstmc.server.level.provider.migration.NukkitItemDataConverter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Nukkit Anvil worlds for conversion into a separate Cloudburst destination.
 */
@ParametersAreNonnullByDefault
public class NukkitAnvilLevelImportSource implements LevelImportSource {

    private final Path levelPath;
    private final Path regionsPath;
    private final Executor executor;
    private final NukkitBlockStateConverter blocks;
    private final NukkitItemDataConverter items;
    private volatile boolean closed;

    public NukkitAnvilLevelImportSource(Path levelPath, Executor executor) throws IOException {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.levelPath = levelPath.toRealPath();
        this.regionsPath = this.levelPath.resolve("region");

        if (!isImportSource(this.levelPath)) {
            throw new IOException("Import source has no Anvil region files: " + levelPath);
        }

        this.blocks = new NukkitBlockStateConverter();
        this.items = new NukkitItemDataConverter();
    }

    public static boolean isImportSource(Path levelPath) throws IOException {
        Path regions = levelPath.resolve("region");
        if (!Files.isDirectory(regions)) {
            return false;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(regions, "*.mca")) {
            for (Path file : files) {
                if (Files.isRegularFile(file)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public CompletableFuture<Void> visitChunks(CloudChunkBuilderFactory factory, Consumer<ImportedChunk> consumer) {
        checkForClosed();

        List<Path> paths;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.regionsPath, "*.mca")) {
            paths = java.util.stream.StreamSupport.stream(stream.spliterator(), false).sorted().toList();
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.runAsync(() -> {
            for (Path path : paths) {
                try (RegionFile regionFile = new RegionFile(path)) {
                    RegionChunkOrigin origin = RegionChunkOrigin.fromPath(path);
                    for (int x = 0; x < 32; x++) {
                        for (int z = 0; z < 32; z++) {
                            if (!regionFile.hasChunk(x, z)) {
                                continue;
                            }

                            int chunkX = origin.x() + x;
                            int chunkZ = origin.z() + z;

                            CloudChunkBuilder builder = factory.create(chunkX, chunkZ);
                            ByteBuf buffer = regionFile.readChunk(x, z);
                            try (ImportedChunk imported = NukkitAnvilConverter.convertToCloudburst(builder, buffer, this.blocks, this.items)) {
                                consumer.accept(imported);
                            } finally {
                                buffer.release();
                            }
                        }
                    }
                } catch (Exception exception) {
                    throw new CompletionException(exception);
                }
            }
        }, Thread::startVirtualThread);
    }

    @Override
    public CompletableFuture<CloudLevelData> loadLevelData(CloudLevelData initialData) {
        checkForClosed();

        return CompletableFuture.supplyAsync(() -> {
            try {
                return NukkitAnvilDataSerializer.INSTANCE.load(initialData, this.levelPath);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, this.executor);
    }

    @Override
    public void close() {
        this.closed = true;
    }

    private void checkForClosed() {
        Preconditions.checkState(!closed, "Import source is closed");
    }

    private record RegionChunkOrigin(int x, int z) {
        private static final Pattern PATTERN = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");

        private static RegionChunkOrigin fromPath(Path regionPath) throws IOException {
            Matcher matcher = PATTERN.matcher(regionPath.getFileName().toString());
            if (!matcher.matches()) {
                throw new IOException("Invalid region file name: " + regionPath);
            }

            try {
                int x = Math.multiplyExact(Integer.parseInt(matcher.group(1)), 32);
                int z = Math.multiplyExact(Integer.parseInt(matcher.group(2)), 32);
                return new RegionChunkOrigin(x, z);
            } catch (IllegalArgumentException | ArithmeticException failure) {
                throw new IOException("Region coordinates are out of range: " + regionPath, failure);
            }
        }
    }
}
