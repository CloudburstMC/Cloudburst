package org.cloudburstmc.server.level.provider.migration;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;

import java.io.IOException;

/**
 * Decodes block, biome, generation and entity data for one source LevelDB encoding.
 */
public interface LevelDBChunkCodec {

    /**
     * Decodes primary and secondary block layers at the absolute section Y coordinate.
     */
    BlockStorage[] readSection(byte[] data, int sectionY) throws IOException;

    /**
     * Resolves a source numeric block ID and metadata, failing if no mapping is supported.
     */
    BlockState readLegacyBlock(int id, int metadata) throws IOException;

    /**
     * Resolves the block reference in a saved pending-tick entry.
     */
    BlockState readTickState(NbtMap tag) throws IOException;

    /**
     * Reads a biome ID from a 16-by-16 column array using local X and Z coordinates.
     */
    int readColumnBiome(byte[] biomes, int x, int z);

    /**
     * Converts a stored height to an absolute block Y coordinate.
     * The encoding may differ between column and three-dimensional biome records.
     */
    int readHeight(int encoded, int minHeight, boolean threeDimensional);

    /**
     * Translates saved generation progress. A missing record means generation is finished.
     */
    ChunkGenerationStatus readGeneration(byte @Nullable [] data) throws IOException;

    /**
     * Converts embedded item and block references without creating a live entity.
     */
    NbtMap convertEntity(NbtMap tag);

    /**
     * Converts embedded item and block references without creating a live block entity.
     */
    NbtMap convertBlockEntity(NbtMap tag);
}
