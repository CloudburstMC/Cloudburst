package org.cloudburstmc.server.level.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.level.chunk.ChunkSection;

import java.util.List;

/**
 * Copies chunks with independent section storage and light arrays.
 */
@UtilityClass
public class TestChunkCopies {

    /**
     * Copies each chunk with independent section storage and light arrays.
     * Immutable block states are shared.
     *
     * @param chunks the chunks to copy
     * @return the copies keyed by {@code CloudChunk.key}
     */
    public static Long2ObjectMap<CloudChunk> copy(Long2ObjectMap<CloudChunk> chunks) {
        Long2ObjectMap<CloudChunk> copies = new Long2ObjectOpenHashMap<>(chunks.size());

        for (Long2ObjectMap.Entry<CloudChunk> entry : chunks.long2ObjectEntrySet()) {
            CloudChunk chunk = entry.getValue();
            copies.put(entry.getLongKey(), copy(chunk));
        }

        return copies;
    }

    private static CloudChunk copy(CloudChunk chunk) {
        ChunkSection[] sections = chunk.getSections();
        CloudChunkSection[] copied = new CloudChunkSection[sections.length];

        for (int i = 0; i < sections.length; i++) {
            copied[i] = sections[i] == null ? null : ((CloudChunkSection) sections[i]).copy();
        }

        UnsafeChunk unsafe = new UnsafeChunk(chunk.getX(), chunk.getZ(), chunk.getLevel(), copied, chunk.getHeightMap(), chunk.getGenerationStatus());
        return new CloudChunk(unsafe, List.of());
    }
}
