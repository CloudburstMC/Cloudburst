package org.cloudburstmc.server.level.provider.anvil;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.LevelImportSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ParametersAreNonnullByDefault
public final class AnvilLevelImportSource implements LevelImportSource {

    private final Path levelPath;
    private final Path regionsPath;
    private final Executor executor;
    private volatile boolean closed;

    public AnvilLevelImportSource(String levelId, Path levelsPath, Executor executor) throws IOException {
        this.executor = executor;
        this.levelPath = levelsPath.resolve(levelId);
        this.regionsPath = this.levelPath.resolve("region");

        Files.createDirectories(regionsPath);
        Preconditions.checkArgument(Files.isDirectory(regionsPath), "region is not a directory");
    }

    @Override
    public CompletableFuture<Void> visitChunks(ChunkBuilder.Factory factory, Consumer<CloudChunk> consumer) {
        checkForClosed();

        List<Path> paths;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.regionsPath, "**.mca")) {
            paths = java.util.stream.StreamSupport.stream(stream.spliterator(), false).toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        CompletableFuture<?>[] futures = paths.stream()
                .map(path -> CompletableFuture.runAsync(() -> {
                    RegionPosition regionPos = RegionPosition.fromPath(path);
                    if (regionPos == null) {
                        return;
                    }
                    try (RegionFile regionFile = new RegionFile(path)) {
                        for (int x = 0; x < 32; x++) {
                            for (int z = 0; z < 32; z++) {
                                if (!regionFile.hasChunk(x, z)) {
                                    continue;
                                }
                                int chunkX = regionPos.x << 5 | x;
                                int chunkZ = regionPos.z << 5 | z;
                                ChunkBuilder builder = factory.create(chunkX, chunkZ);
                                ByteBuf buffer = regionFile.readChunk(x, z);
                                try {
                                    AnvilConverter.convertToCloudburst(builder, buffer);
                                } finally {
                                    buffer.release();
                                }
                                consumer.accept(builder.build());
                            }
                        }
                    } catch (Exception exception) {
                        throw new CompletionException(exception);
                    }
                }, this.executor))
                .toArray(CompletableFuture<?>[]::new);

        return CompletableFuture.allOf(futures);
    }

    @Override
    public CompletableFuture<Optional<CloudLevelData>> loadLevelData(CloudLevelData initialData) {
        checkForClosed();

        return CompletableFuture.supplyAsync(() -> {
            try {
                return AnvilDataSerializer.INSTANCE.load(initialData, this.levelPath);
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
        Preconditions.checkState(!closed, "LevelProvider closed");
    }

    private record RegionPosition(int x, int z) {
        private static final Pattern PATTERN = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");

        private static @Nullable RegionPosition fromPath(Path regionPath) {
            Matcher matcher = PATTERN.matcher(regionPath.getFileName().toString());
            if (!matcher.matches()) {
                return null;
            }
            int x = Integer.parseInt(matcher.group(1));
            int z = Integer.parseInt(matcher.group(2));
            return new RegionPosition(x, z);
        }
    }
}
