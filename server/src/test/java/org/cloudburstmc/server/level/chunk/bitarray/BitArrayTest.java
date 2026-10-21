package org.cloudburstmc.server.level.chunk.bitarray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BitArrayTest {

    @Test
    void selectsTheSmallestSupportedWidthAtEveryCapacityBoundary() {
        int previousCapacity = 0;
        for (BitArrayVersion version : BitArrayVersion.values()) {
            assertSame(version, BitArrayVersion.fromBits(version.getBits()));
            assertSame(version, BitArrayVersion.forPaletteSize(previousCapacity + 1));
            assertSame(version, BitArrayVersion.forPaletteSize(version.getMaxValue() + 1));
            assertEquals(Integer.SIZE / version.getBits(), version.getEntriesPerWord());
            previousCapacity = version.getMaxValue() + 1;
        }

        for (int bits : new int[]{-1, 0, 7, 9, 15, 17, 32, 127}) {
            assertThrows(IllegalArgumentException.class, () -> BitArrayVersion.fromBits(bits));
        }

        for (int size : new int[]{-1, 0, 65537, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> BitArrayVersion.forPaletteSize(size));
        }
    }

    @Test
    void entriesAndRawWordsAgreeAcrossWordBoundariesForEveryLayout() {
        for (BitArrayVersion version : BitArrayVersion.values()) {
            int entriesPerWord = version.getEntriesPerWord();
            int[] sizes = {0, 1, entriesPerWord - 1, entriesPerWord, entriesPerWord + 1, 4096};
            for (int size : sizes) {
                BitArray array = version.createArray(size);
                int[] expectedWords = new int[(int) Math.ceilDiv((long) size, entriesPerWord)];

                assertEquals(size, array.size());
                assertSame(version, array.getVersion());
                assertEquals(expectedWords.length, array.getWords().length);

                for (int index = 0; index < size; index++) {
                    int value = (index & 1) == 0 ? version.getMaxValue() : (index * 31) & version.getMaxValue();
                    array.set(index, value);
                    expectedWords[index / entriesPerWord] |= value << ((index % entriesPerWord) * version.getBits());
                }

                assertArrayEquals(expectedWords, array.getWords());
                for (int index = 0; index < size; index++) {
                    int expected = (index & 1) == 0 ? version.getMaxValue() : (index * 31) & version.getMaxValue();
                    assertEquals(expected, array.get(index));
                }

                if (size > 0) {
                    array.set(size - 1, 0);
                    assertEquals(0, array.get(size - 1));
                }
            }
        }
    }

    @Test
    void invalidIndicesValuesAndWordCountsAreRejectedWithoutChangingEntries() {
        for (BitArrayVersion version : BitArrayVersion.values()) {
            int size = version.getEntriesPerWord() + 1;
            BitArray array = version.createArray(size);
            array.set(0, version.getMaxValue());
            int[] expected = array.getWords().clone();

            assertThrows(IndexOutOfBoundsException.class, () -> array.get(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> array.get(size));
            assertThrows(IndexOutOfBoundsException.class, () -> array.set(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> array.set(size, 0));
            assertThrows(IllegalArgumentException.class, () -> array.set(0, -1));
            assertThrows(IllegalArgumentException.class, () -> array.set(0, version.getMaxValue() + 1));
            assertThrows(IllegalArgumentException.class, () -> version.createArray(-1));
            assertThrows(IllegalArgumentException.class, () -> version.createArray(size, new int[0]));
            assertThrows(IllegalArgumentException.class,
                    () -> version.createArray(size, new int[version.wordCount(size) + 1]));
            assertThrows(NullPointerException.class, () -> version.createArray(size, null));
            assertArrayEquals(expected, array.getWords());
        }
    }

    @Test
    void copiesOwnTheirBackingWords() {
        for (BitArrayVersion version : BitArrayVersion.values()) {
            BitArray array = version.createArray(40);
            array.set(39, version.getMaxValue());
            BitArray copy = array.copy();
            copy.set(39, 0);

            assertNotSame(array.getWords(), copy.getWords());
            assertEquals(version.getMaxValue(), array.get(39));
            assertEquals(0, copy.get(39));
        }
    }

    @Test
    void wordCountsUseIntegerRoundingWithoutOverflow() {
        for (BitArrayVersion version : BitArrayVersion.values()) {
            assertEquals(0, version.wordCount(0));
            assertEquals(Math.ceilDiv((long) Integer.MAX_VALUE, version.getEntriesPerWord()),
                    version.wordCount(Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> version.wordCount(-1));
        }
    }
}
