package org.cloudburstmc.server.level.chunk;

import org.cloudburstmc.api.block.BlockState;

import java.util.Arrays;

/**
 * Compact bidirectional index of randomly-ticking block positions within a
 * single 16×16×16 chunk section.
 *
 * <p>Two parallel structures are maintained in sync:
 * <ul>
 *   <li>{@code posToIndex}: flat {@code int[4096]} direct-mapped array.
 *       {@code posToIndex[key]} gives the sequential index of position
 *       {@code key} in {@code entries}, or -1 when absent. Direct array
 *       access is O(1) with no hashing overhead.</li>
 *   <li>{@code entries}: a dense {@code long[]} array where each element
 *       packs the position key into bits 15..0. Sequential array access lets
 *       the random-tick loop pick an entry in O(1) without any palette lookup
 *       for rolls that fall outside the ticking set.</li>
 * </ul>
 *
 * <p>The actual {@link BlockState} reference is stored separately in
 * {@code stateByIndex} so we can return it cheaply from
 * {@link #getState(int)} without any unpacking.
 */
public final class SectionTickList {

    /**
     * Maximum positions in a section.
     */
    private static final int SECTION_SIZE = 4096;

    /**
     * Direct-mapped reverse index: {@code posToIndex[key]} gives the
     * sequential index of position {@code key} in {@code entries}, or -1
     * when absent. Always exactly {@code SECTION_SIZE} elements.
     */
    private final int[] posToIndex;

    /**
     * Packed long entries: bits 15..0 = 12-bit position key.
     */
    private long[] entries;

    /**
     * Parallel array: the BlockState at each sequential index.
     */
    private BlockState[] stateByIndex;

    /**
     * Number of entries currently in the list.
     */
    private int size;

    public SectionTickList() {
        this.posToIndex = new int[SECTION_SIZE];
        Arrays.fill(this.posToIndex, -1);
        this.entries = new long[8];
        this.stateByIndex = new BlockState[8];
        this.size = 0;
    }

    /**
     * Adds the block at intra-section position {@code (x, y, z)} with the
     * given state. Does nothing if the position is already tracked.
     *
     * @param x     0-15
     * @param y     0-15
     * @param z     0-15
     * @param state the ticking block state at this position
     */
    public void add(int x, int y, int z, BlockState state) {
        int key = posKey(x, y, z);
        if (posToIndex[key] >= 0) {
            return;
        }
        int idx = size;
        ensureCapacity(idx + 1);
        entries[idx] = packEntry(key, state);
        stateByIndex[idx] = state;
        posToIndex[key] = idx;
        size++;
    }

    /**
     * Removes the entry at intra-section position {@code (x, y, z)}.
     * Uses swap-with-tail so the dense array stays contiguous.
     *
     * @param x 0-15
     * @param y 0-15
     * @param z 0-15
     */
    public void remove(int x, int y, int z) {
        int key = posKey(x, y, z);
        int idx = posToIndex[key];
        if (idx < 0) {
            return;
        }

        posToIndex[key] = -1;
        int last = size - 1;
        if (idx != last) {
            long tailEntry = entries[last];
            BlockState tailState = stateByIndex[last];
            entries[idx] = tailEntry;
            stateByIndex[idx] = tailState;
            int tailKey = (int) (tailEntry & 0xFFFF);
            posToIndex[tailKey] = idx;
        }

        entries[last] = 0L;
        stateByIndex[last] = null;
        size--;
    }

    /**
     * Removes all entries.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            int key = (int) (entries[i] & 0xFFFF);
            posToIndex[key] = -1;
            entries[i] = 0L;
            stateByIndex[i] = null;
        }
        size = 0;
    }

    /**
     * Returns the number of tracked ticking blocks.
     */
    public int size() {
        return size;
    }

    /**
     * Returns {@code true} when no ticking blocks are tracked.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the {@link BlockState} at sequential index {@code idx}.
     * Valid range: {@code 0 <= idx < size()}.
     */
    public BlockState getState(int idx) {
        return stateByIndex[idx];
    }

    /**
     * Decodes the intra-section x coordinate from sequential entry {@code idx}.
     */
    public int getX(int idx) {
        return (int) ((entries[idx] >> 8) & 0xF);
    }

    /**
     * Decodes the intra-section y coordinate from sequential entry {@code idx}.
     */
    public int getY(int idx) {
        return (int) (entries[idx] & 0xF);
    }

    /**
     * Decodes the intra-section z coordinate from sequential entry {@code idx}.
     */
    public int getZ(int idx) {
        return (int) ((entries[idx] >> 4) & 0xF);
    }

    private void ensureCapacity(int minSize) {
        if (minSize <= entries.length) {
            return;
        }
        int newLen = Math.min(entries.length << 1, SECTION_SIZE);
        entries = java.util.Arrays.copyOf(entries, newLen);
        stateByIndex = java.util.Arrays.copyOf(stateByIndex, newLen);
    }

    /**
     * 12-bit intra-section position key in range [0, 4096).
     * Encoding: {@code (x<<8)|(z<<4)|y}, matching
     * {@link CloudChunkSection#blockIndex(int, int, int)}.
     */
    private static int posKey(int x, int y, int z) {
        return (x << 8) | (z << 4) | y;
    }

    /**
     * Packs the position key into bits 15..0 of the entry long.
     * State is kept separately in {@code stateByIndex}.
     */
    private static long packEntry(int key, BlockState state) {
        return key & 0xFFFFL;
    }
}
