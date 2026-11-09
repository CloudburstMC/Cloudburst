package org.cloudburstmc.server.level.provider.migration;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Translates Nukkit generation markers to destination generation status.
 */
@UtilityClass
public class NukkitChunkGeneration {

    /**
     * Reads a LevelDB finalization record. A missing record means generation is finished.
     */
    public static ChunkGenerationStatus readLevelDB(byte @Nullable [] data) throws IOException {
        if (data == null) {
            return ChunkGenerationStatus.FINISHED;
        }

        if (data.length != Integer.BYTES) {
            throw new IOException("Invalid Nukkit finalization state");
        }

        int state = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
        return switch (state) {
            case -1 -> ChunkGenerationStatus.NEW;
            case 0 -> ChunkGenerationStatus.GENERATED;
            case 1, 2 -> ChunkGenerationStatus.FINISHED;
            default -> throw new IOException("Unknown Nukkit finalization state: " + state);
        };
    }

    /**
     * Reads Anvil generation flags, giving completed population precedence over generation.
     */
    public static ChunkGenerationStatus readAnvil(NbtMap tag) {
        if (tag.getBoolean("TerrainPopulated")) {
            return ChunkGenerationStatus.FINISHED;
        }

        return tag.getBoolean("TerrainGenerated") ? ChunkGenerationStatus.GENERATED : ChunkGenerationStatus.NEW;
    }
}
