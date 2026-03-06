package org.cloudburstmc.server.level.chunk.bitarray;

import org.cloudburstmc.server.math.MathHelper;

import java.util.Arrays;

public class PaddedBitArray implements BitArray {

    /**
     * Array used to store data
     */
    private final int[] words;

    /**
     * Palette version information
     */
    private final BitArrayVersion version;

    /**
     * Number of entries in this palette (<b>not</b> the length of the words array that internally backs this palette)
     */
    private final int size;

    PaddedBitArray(BitArrayVersion version, int size, int[] words) {
        this.size = size;
        this.version = version;
        this.words = words;
        int expectedWordsLength = MathHelper.ceil((float) size / version.entriesPerWord);
        if (words.length != expectedWordsLength) {
            throw new IllegalArgumentException("Invalid length given for storage, got: " + words.length +
                    " but expected: " + expectedWordsLength);
        }
    }

    /**
     * Sets the entry at the given location to the given value.
     * Bounds checks are omitted intentionally. All callers are trusted to pass
     * valid indices (0 ≤ index < size) and values (0 ≤ value ≤ maxEntryValue).
     */
    @Override
    public void set(int index, int value) {
        int arrayIndex = index / this.version.entriesPerWord;
        int offset = (index % this.version.entriesPerWord) * this.version.bits;

        this.words[arrayIndex] = this.words[arrayIndex] & ~(this.version.maxEntryValue << offset) | (value & this.version.maxEntryValue) << offset;
    }

    /**
     * Gets the entry at the given index.
     * Bounds checks are omitted intentionally. Callers are trusted to pass
     * valid indices (0 ≤ index < size).
     */
    @Override
    public int get(int index) {
        int arrayIndex = index / this.version.entriesPerWord;
        int offset = (index % this.version.entriesPerWord) * this.version.bits;

        return (this.words[arrayIndex] >>> offset) & this.version.maxEntryValue;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public int[] getWords() {
        return this.words;
    }

    @Override
    public BitArrayVersion getVersion() {
        return this.version;
    }

    @Override
    public BitArray copy() {
        return new PaddedBitArray(this.version, this.size, Arrays.copyOf(this.words, this.words.length));
    }
}
