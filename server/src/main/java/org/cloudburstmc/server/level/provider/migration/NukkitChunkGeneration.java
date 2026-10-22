package org.cloudburstmc.server.level.provider.migration;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Translates Nukkit generation progress without replaying completed terrain decoration.
 */
@UtilityClass
public class NukkitChunkGeneration {

    public static ChunkGenerationStatus readLevelDB(byte[] data) throws IOException {
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

    public static ChunkGenerationStatus readAnvil(NbtMap tag) {
        if (tag.getBoolean("TerrainPopulated")) {
            return ChunkGenerationStatus.FINISHED;
        }

        return tag.getBoolean("TerrainGenerated") ? ChunkGenerationStatus.GENERATED : ChunkGenerationStatus.NEW;
    }
}
