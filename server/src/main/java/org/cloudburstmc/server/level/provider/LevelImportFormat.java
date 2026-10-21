package org.cloudburstmc.server.level.provider;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.util.stream.LittleEndianDataInputStream;
import org.cloudburstmc.server.level.provider.anvil.AnvilLevelImportSource;
import org.cloudburstmc.server.level.provider.migration.NukkitLevelDBImportSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

/**
 * Source formats converted before a level is opened for normal use.
 */
public enum LevelImportFormat {
    NUKKIT_LEVELDB,
    NUKKIT_ANVIL;

    public static @Nullable LevelImportFormat detect(Path source) throws IOException {
        Path backup = source.resolveSibling("." + source.getFileName() + ".nukkit-backup");
        if (!Files.exists(source) && Files.exists(backup)) {
            throw new IOException("Interrupted level conversion. Restore " + backup + " to " + source + " before loading");
        }

        if (AnvilLevelImportSource.isImportSource(source)) {
            return NUKKIT_ANVIL;
        }

        Path metadata = source.resolve("level.dat");
        if (!Files.isDirectory(source.resolve("db"))) {
            return null;
        }

        NbtMap tag;
        try {
            tag = readMetadata(metadata);
        } catch (IOException failure) {
            Path previous = source.resolve("level.dat_old");
            if (!Files.exists(metadata) && !Files.exists(previous)) {
                return null;
            }
            try {
                tag = readMetadata(previous);
            } catch (IOException backupFailure) {
                failure.addSuppressed(backupFailure);
                throw failure;
            }
        }

        String generator = tag.getString("generatorName");
        return !generator.isEmpty() && generator.indexOf(':') < 0 && tag.containsKey("generatorOptions") && !tag.containsKey("CloudburstClock") ? NUKKIT_LEVELDB : null;
    }

    private static NbtMap readMetadata(Path metadata) throws IOException {
        try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream(Files.newInputStream(metadata)); NBTInputStream reader = new NBTInputStream(stream)) {
            int version = stream.readInt();
            int length = stream.readInt();

            if (version < 8 || version > 12 || length != Files.size(metadata) - 8) {
                throw new IOException("Invalid LevelDB level.dat header: " + metadata);
            }

            if (!(reader.readTag() instanceof NbtMap tag)) {
                throw new IOException("level.dat must contain an NBT compound: " + metadata);
            }

            return tag;
        }
    }

    public LevelImportSource open(Path source, int dimension, Executor executor) throws IOException {
        return switch (this) {
            case NUKKIT_LEVELDB -> new NukkitLevelDBImportSource(source, dimension, executor);
            case NUKKIT_ANVIL -> new AnvilLevelImportSource(source, executor);
        };
    }
}
