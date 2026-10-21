package org.cloudburstmc.server.level.chunk.bitarray;

/**
 * Fixed-size nonnegative integers packed into 32-bit words.
 */
public interface BitArray {

    /**
     * Replaces an entry, rejecting indices outside the array and values outside its bit width.
     */
    void set(int index, int value);

    /**
     * Returns an entry, rejecting indices outside the array.
     */
    int get(int index);

    /**
     * Returns the number of entries, not the word count.
     */
    int size();

    /**
     * Returns the backing words for serialization. Callers must not modify them.
     */
    int[] getWords();

    BitArrayVersion getVersion();

    /**
     * Returns an independent copy of the entries and backing words.
     */
    BitArray copy();
}
