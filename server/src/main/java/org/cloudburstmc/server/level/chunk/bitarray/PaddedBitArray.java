package org.cloudburstmc.server.level.chunk.bitarray;

import java.util.Objects;

/**
 * Packs entries without crossing word boundaries, leaving unused high bits in each word.
 */
public class PaddedBitArray implements BitArray {

    private final int[] words;
    private final BitArrayVersion version;
    private final int size;

    PaddedBitArray(BitArrayVersion version, int size, int[] words) {
        this.version = version;
        this.size = size;
        this.words = words;
    }

    @Override
    public int get(int index) {
        Objects.checkIndex(index, this.size);
        int wordIndex = index / this.version.getEntriesPerWord();
        int offset = (index % this.version.getEntriesPerWord()) * this.version.getBits();
        return (this.words[wordIndex] >>> offset) & this.version.getMaxValue();
    }

    @Override
    public void set(int index, int value) {
        Objects.checkIndex(index, this.size);
        if (value < 0 || value > this.version.getMaxValue()) {
            throw new IllegalArgumentException("Value " + value + " does not fit " + this.version.getBits() + " bits");
        }

        int wordIndex = index / this.version.getEntriesPerWord();
        int offset = (index % this.version.getEntriesPerWord()) * this.version.getBits();
        int mask = this.version.getMaxValue() << offset;
        this.words[wordIndex] = (this.words[wordIndex] & ~mask) | (value << offset);
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
        return new PaddedBitArray(this.version, this.size, this.words.clone());
    }
}
