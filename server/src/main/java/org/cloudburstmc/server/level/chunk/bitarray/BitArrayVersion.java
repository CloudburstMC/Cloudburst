package org.cloudburstmc.server.level.chunk.bitarray;

import lombok.Getter;

import java.util.Objects;

/**
 * Supported nonzero bit widths for indices packed into 32-bit words.
 * Entries never cross word boundaries.
 */
@Getter
public enum BitArrayVersion {
    V1(1),
    V2(2),
    V3(3),
    V4(4),
    V5(5),
    V6(6),
    V8(8),
    V16(16);

    private static final BitArrayVersion[] VERSIONS = values();

    private final int bits;
    private final int entriesPerWord;
    private final int maxValue;

    BitArrayVersion(int bits) {
        this.bits = bits;
        this.entriesPerWord = Integer.SIZE / bits;
        this.maxValue = (1 << bits) - 1;
    }

    /**
     * Resolves an encoded nonzero bit width.
     */
    public static BitArrayVersion fromBits(int bits) {
        return switch (bits) {
            case 1 -> V1;
            case 2 -> V2;
            case 3 -> V3;
            case 4 -> V4;
            case 5 -> V5;
            case 6 -> V6;
            case 8 -> V8;
            case 16 -> V16;
            default -> throw new IllegalArgumentException("Unsupported palette bit width: " + bits);
        };
    }

    /**
     * Selects the smallest nonzero layout for one through 65536 palette entries.
     * Singleton storage may omit the index array entirely.
     */
    public static BitArrayVersion forPaletteSize(int paletteSize) {
        if (paletteSize < 1 || paletteSize > V16.maxValue + 1) {
            throw new IllegalArgumentException("Invalid palette size: " + paletteSize);
        }

        for (BitArrayVersion version : VERSIONS) {
            if (paletteSize - 1 <= version.maxValue) {
                return version;
            }
        }

        throw new IllegalStateException("No layout for palette size " + paletteSize);
    }

    /**
     * Returns the word count for a nonnegative number of entries, including a partial final word.
     */
    public int wordCount(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative bit-array size: " + size);
        }

        return Math.ceilDiv(size, this.entriesPerWord);
    }

    public BitArray createArray(int size) {
        return this.createArray(size, new int[this.wordCount(size)]);
    }

    /**
     * Creates an array taking ownership of the supplied words.
     * Their length must exactly match the layout's word count.
     */
    public BitArray createArray(int size, int[] words) {
        Objects.requireNonNull(words, "words");
        int expected = this.wordCount(size);
        if (words.length != expected) {
            throw new IllegalArgumentException("Expected " + expected + " packed words, got " + words.length);
        }

        return (Integer.SIZE % this.bits) == 0
                ? new Pow2BitArray(this, size, words)
                : new PaddedBitArray(this, size, words
        );
    }
}
