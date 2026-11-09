package org.cloudburstmc.server.level.chunk;

/**
 * Borrowed section access for lighting while the owning chunk's write lock is held.
 * This view does not own the lock and must not outlive it.
 */
public interface ChunkLightView {

    /**
     * Returns a section-array snapshot whose entries may be null.
     * The section objects remain shared with the chunk.
     *
     * @throws IllegalStateException if the view is closed or read-only
     */
    CloudChunkSection[] getLightSections();

    /**
     * Returns or creates a section without marking the chunk dirty.
     *
     * @param sectionIndex index relative to the level's minimum section
     * @return the writable section
     * @throws IllegalStateException if the view is closed or read-only
     */
    CloudChunkSection getOrCreateLightSection(int sectionIndex);
}
