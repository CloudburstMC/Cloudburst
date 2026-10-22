package org.cloudburstmc.server.level.provider.migration;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.level.provider.leveldb.serializer.LevelDBDataSerializer;

import java.io.IOException;

/**
 * Identifies the saved data encoding within a LevelDB world.
 */
public enum LevelDBImportProfile {
    CLOUDBURST,
    BEDROCK,
    NUKKIT;

    /**
     * Detects native storage first, then applies an explicit source profile or known metadata markers.
     * Unrecognized metadata requires an explicit profile.
     */
    public static LevelDBImportProfile detect(NbtMap metadata, @Nullable LevelDBImportProfile selected) throws IOException {
        if (metadata.containsKey("CloudburstStorageVersion")) {
            if (!metadata.containsKey("CloudburstStorageVersion", NbtType.INT)
                    || metadata.getInt("CloudburstStorageVersion") != LevelDBDataSerializer.CLOUDBURST_STORAGE_VERSION) {
                throw new IOException("Unsupported Cloudburst storage version");
            }

            return CLOUDBURST;
        }

        if (metadata.containsKey("CloudburstClock")) {
            throw new IOException("Unversioned Cloudburst storage is no longer supported");
        }

        if (selected != null) {
            if (selected == CLOUDBURST) {
                throw new IOException("Cloudburst storage cannot be selected as an import profile");
            }

            return selected;
        }

        if (metadata.containsKey("PM1EGen", NbtType.INT)) {
            return NUKKIT;
        }

        if (metadata.containsKey("lastOpenedWithVersion", NbtType.LIST) || metadata.containsKey("MinimumCompatibleClientVersion", NbtType.LIST)) {
            return BEDROCK;
        }

        throw new IOException("Unrecognized LevelDB encoding. Select import-profile: bedrock or nukkit for this level after verifying its encoding");
    }

    /**
     * Creates a source decoder. Native Cloudburst storage is rejected because it needs no import.
     */
    public LevelDBChunkCodec createCodec() throws IOException {
        return switch (this) {
            case NUKKIT -> new NukkitLevelDBChunkCodec();
            case BEDROCK -> new BedrockLevelDBChunkCodec();
            case CLOUDBURST -> throw new IOException("Cloudburst storage does not require import");
        };
    }
}
