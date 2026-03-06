package org.cloudburstmc.server.level.chunk;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Per-section 3D biome storage backed by a paletted {@link BitArray}.
 * <p>
 * The index mapping is identical to {@link CloudChunkSection#blockIndex(int, int, int)}:
 * {@code index = (x << 8) | (z << 4) | y}, where x, y, z are each 0–15 within the section.
 * <p>
 * Wire and disk format mirrors the {@link BlockStorage} paletted format, but uses raw
 * integer biome IDs in the palette rather than NBT. The copy-last optimization is
 * available via the {@code previous} parameter on {@link #writeToNetwork} and {@link #writeToDisk}.
 */
public class BiomeStorage {

    /**
     * Number of entries in one section: 16 * 16 * 16.
     */
    static final int SIZE = 4096;

    private final IntList palette;
    private BitArray bitArray;

    /**
     * Create a new storage defaulting every position to biome ID {@code defaultBiomeId}.
     * Starts as a true 0-bit singleton (no word array is allocated until a second
     * biome ID is introduced via {@link #setBiome}.
     */
    public BiomeStorage(int defaultBiomeId) {
        this.bitArray = null;
        this.palette = new IntArrayList(4);
        this.palette.add(defaultBiomeId);
    }

    private BiomeStorage(BitArray bitArray, IntList palette) {
        this.bitArray = bitArray;
        this.palette = palette;
    }

    /**
     * Returns the biome ID at the given intra-section position.
     *
     * @param index pre-computed {@code (x << 8) | (z << 4) | y}
     */
    public int getBiome(int index) {
        if (this.bitArray == null) {
            return this.palette.getInt(0);
        }
        return this.palette.getInt(this.bitArray.get(index));
    }

    /**
     * Sets the biome ID at the given intra-section index.
     *
     * @param index   pre-computed {@code (x << 8) | (z << 4) | y}
     * @param biomeId raw biome integer ID
     */
    public void setBiome(int index, int biomeId) {
        int paletteIdx = this.palette.indexOf(biomeId);
        if (paletteIdx == -1) {
            paletteIdx = this.palette.size();
            if (this.bitArray == null) {
                // Promote from 0-bit singleton to V1 now that we have a second value
                this.bitArray = BitArrayVersion.V1.createPalette(SIZE);
            } else {
                BitArrayVersion version = this.bitArray.getVersion();
                if (paletteIdx > version.getMaxEntryValue()) {
                    BitArrayVersion next = version.next();
                    if (next != null) {
                        this.onResize(next);
                    }
                }
            }
            this.palette.add(biomeId);
        }

        if (this.bitArray != null) {
            this.bitArray.set(index, paletteIdx);
        }

        // paletteIdx == 0 in a 0-bit singleton: no-op (all positions already map to 0)
    }

    /**
     * Writes this storage to a network buffer.
     * If {@code previous} is non-null and the two storages are palette-equal,
     * writes a single {@code 0xFF} copy-last byte instead of the full payload.
     * <p>
     * Copy-last is suppressed when the first palette entry of either storage is 0
     * (biome ID 0 = Ocean) to prevent client-side chunk decoding errors.
     */
    public void writeToNetwork(ByteBuf buf, @Nullable BiomeStorage previous) {
        if (previous != null && this.palette.getInt(0) != 0 && previous.palette.getInt(0) != 0 && this.equalsPalette(previous)) {
            buf.writeByte(0xFF);
            return;
        }
        writePalettedStorage(buf, true);
    }

    /**
     * Writes this storage to a disk buffer.
     * Applies the same copy-last optimization as the network path.
     * <p>
     * The copy-last sentinel on disk is {@code 0xFE} (= {@code (0x7f << 1) | 0}).
     * Copy-last is suppressed when the first palette entry of either storage is 0.
     */
    public void writeToDisk(ByteBuf buf, @Nullable BiomeStorage previous) {
        if (previous != null && this.palette.getInt(0) != 0 && previous.palette.getInt(0) != 0
                && this.equalsPalette(previous)) {
            buf.writeByte(0xFE);
            return;
        }
        writePalettedStorage(buf, false);
    }

    /**
     * Read a storage previously written by {@link #writeToDisk}.
     */
    public static BiomeStorage readFromDisk(ByteBuf buf, @Nullable BiomeStorage previous) {
        int header = buf.readUnsignedByte();
        if ((header >> 1) == 0x7F) {
            if (previous == null) {
                throw new IllegalStateException("Got copy-last (0xFE/0xFF) header but no previous section");
            }
            return previous.copy();
        }
        return readPalettedStorage(buf, header, false);
    }

    /**
     * Writes the full paletted payload.
     *
     * <p>If this storage is a singleton, writes the compact 0-bit format: a header byte of
     * {@code (0 << 1) | runtimeBit} followed by exactly one palette entry and no word array.
     *
     * @param runtime {@code true} sets the low bit in the header (network); {@code false} clears it (disk)
     */
    private void writePalettedStorage(ByteBuf buf, boolean runtime) {
        if (isSingleValue()) {
            // 0-bit singleton: header = (0 << 1) | runtimeBit, no words, one palette entry, no count field
            buf.writeByte(runtime ? 0x01 : 0x00);
            if (runtime) {
                VarInts.writeInt(buf, this.palette.getInt(0));
            } else {
                buf.writeIntLE(this.palette.getInt(0));
            }
            return;
        }

        BitArrayVersion version = this.bitArray.getVersion();
        int header = (version.getId() << 1) | (runtime ? 1 : 0);
        buf.writeByte(header);

        for (int word : this.bitArray.getWords()) {
            buf.writeIntLE(word);
        }

        int paletteSize = this.palette.size();
        if (runtime) {
            VarInts.writeInt(buf, paletteSize);
            for (int i = 0; i < paletteSize; i++) {
                VarInts.writeInt(buf, this.palette.getInt(i));
            }
        } else {
            buf.writeIntLE(paletteSize);
            for (int i = 0; i < paletteSize; i++) {
                buf.writeIntLE(this.palette.getInt(i));
            }
        }
    }

    private static BiomeStorage readPalettedStorage(ByteBuf buf, int header, boolean runtime) {
        int bitsPerEntry = header >> 1;

        // 0-bit singleton: no words, no count, just one palette entry
        if (bitsPerEntry == 0) {
            int id = runtime ? VarInts.readInt(buf) : buf.readIntLE();
            return new BiomeStorage(id);
        }

        BitArrayVersion version = BitArrayVersion.get(bitsPerEntry, true);

        int wordCount = version.getWordsForSize(SIZE);
        int[] words = new int[wordCount];
        for (int i = 0; i < wordCount; i++) {
            words[i] = buf.readIntLE();
        }
        BitArray bitArray = version.createPalette(SIZE, words);

        IntList palette = new IntArrayList(4);
        int paletteSize = runtime ? VarInts.readInt(buf) : buf.readIntLE();

        checkArgument(version.getMaxEntryValue() >= paletteSize - 1,
                "Biome palette is too large. Max %s, actual %s", version.getMaxEntryValue(), paletteSize);

        for (int i = 0; i < paletteSize; i++) {
            int id = runtime ? VarInts.readInt(buf) : buf.readIntLE();
            palette.add(id);
        }

        return new BiomeStorage(bitArray, palette);
    }

    private void onResize(BitArrayVersion version) {
        BitArray newBitArray = version.createPalette(SIZE);
        if (this.bitArray != null) {
            for (int i = 0; i < SIZE; i++) {
                newBitArray.set(i, this.bitArray.get(i));
            }
        }

        // if bitArray was null (0-bit singleton), newBitArray is all-zeros, correct
        // since all 4096 entries pointed to palette index 0.
        this.bitArray = newBitArray;
    }

    /**
     * Returns {@code true} if both storages represent exactly the same set of
     * biome IDs at every position. Used for the copy-last optimization.
     * <p>
     * Handles the case where two logically-identical storages are backed by
     * {@link BitArray}s of different bit widths (e.g., one at V1 and one at V2
     * that are both effectively all-zeros with a 1-entry palette).
     */
    public boolean equalsPalette(BiomeStorage other) {
        if (this == other) {
            return true;
        }

        // Fast path: both are singletons, just compare the single palette entry
        if (this.isSingleValue() && other.isSingleValue()) {
            return this.palette.getInt(0) == other.palette.getInt(0);
        }

        // Both must be non-singleton for a word-level comparison to make sense
        if (this.isSingleValue() || other.isSingleValue()) {
            return false;
        }

        if (!this.palette.equals(other.palette)) {
            return false;
        }

        // compare index words directly (same bit-width guaranteed by same palette size)
        int[] w1 = this.bitArray.getWords();
        int[] w2 = other.bitArray.getWords();
        if (w1.length != w2.length) {
            return false;
        }

        for (int i = 0; i < w1.length; i++) {
            if (w1[i] != w2[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a deep copy of this storage.
     */
    public BiomeStorage copy() {
        return new BiomeStorage(this.bitArray != null ? this.bitArray.copy() : null, new IntArrayList(this.palette));
    }

    /**
     * Returns {@code true} if all positions share the same single palette entry.
     */
    public boolean isSingleValue() {
        if (this.bitArray == null) {
            return true;
        }

        if (this.palette.size() == 1) {
            return true;
        }

        for (int word : this.bitArray.getWords()) {
            if (Integer.toUnsignedLong(word) != 0L) {
                return false;
            }
        }

        return true;
    }
}
