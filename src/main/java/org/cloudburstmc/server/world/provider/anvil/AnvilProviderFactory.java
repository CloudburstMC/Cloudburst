package org.cloudburstmc.server.world.provider.anvil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.world.WorldConverter;
import org.cloudburstmc.server.world.provider.WorldProvider;
import org.cloudburstmc.server.world.provider.WorldProviderFactory;
import org.cloudburstmc.server.world.provider.leveldb.LevelDBProviderFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnvilProviderFactory implements WorldProviderFactory {

    public static final AnvilProviderFactory INSTANCE = new AnvilProviderFactory();

    @Override
    public WorldProvider create(String levelId, Path levelsPath, Executor executor) throws IOException {
        try (WorldProvider oldProvider = new AnvilProvider(levelId, levelsPath, executor);
             WorldProvider newProvider = LevelDBProviderFactory.INSTANCE.create(levelId, levelsPath, executor)) {

            WorldConverter converter = new WorldConverter(oldProvider, newProvider);
            converter.perform().join();
        }

        try (Stream<Path> walk = Files.walk(levelsPath.resolve(levelId).resolve("region"))) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete region/ directory");
        }

        return LevelDBProviderFactory.INSTANCE.create(levelId, levelsPath, executor);
    }

    @Override
    public boolean isCompatible(String levelId, Path levelsPath) {
        Path levelPath = levelsPath.resolve(levelId);
        if (Files.isDirectory(levelPath)) {
            Path regionPath = levelPath.resolve("region");
            if (Files.isDirectory(regionPath)) {
                Visitor visitor = new Visitor();
                try {
                    Files.walkFileTree(regionPath, visitor);
                    return visitor.found;
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return false;
    }

    private static class Visitor extends SimpleFileVisitor<Path> {
        private static final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.mca");

        private boolean found;

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            if (matcher.matches(path)) {
                found = true;
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
