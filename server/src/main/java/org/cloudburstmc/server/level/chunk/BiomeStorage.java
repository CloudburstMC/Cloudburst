package org.cloudburstmc.server.level.chunk;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;

import java.util.Arrays;
import java.util.Objects;

/**
 * Stores one 16-by-16-by-16 biome layer using integer palette entries.
 * A singleton palette has no index array. Instances are not thread-safe.
 * Buffer methods read or append one layer without releasing the supplied buffer.
 */
public class BiomeStorage {

    private static final int SIZE = 4096;
    private static final int COPY_PREVIOUS_BITS = 0x7F;

    private final IntList palette;
    private final Int2IntOpenHashMap paletteIndex;
    private @Nullable BitArray bitArray;
    private boolean needsCompaction;

    /**
     * Creates a layer filled with the supplied biome ID.
     * Biome IDs are stored without registry validation.
     */
    public BiomeStorage(int defaultBiomeId) {
        this(null, IntArrayList.of(defaultBiomeId), false);
    }

    private BiomeStorage(@Nullable BitArray bitArray, IntList palette, boolean needsCompaction) {
        this.bitArray = bitArray;
        this.palette = palette;
        this.paletteIndex = new Int2IntOpenHashMap(palette.size());
        this.paletteIndex.defaultReturnValue(-1);
        this.rebuildPaletteIndex();
        this.needsCompaction = needsCompaction;
    }

    /**
     * Returns the biome ID at {@code (x << 8) | (z << 4) | y} within the layer.
     * Each local coordinate ranges from 0 to 15.
     *
     * @throws IndexOutOfBoundsException if the index is outside 0 to 4095
     */
    public int getBiome(int index) {
        Objects.checkIndex(index, SIZE);
        return this.palette.getInt(this.paletteIndexAt(index));
    }

    /**
     * Replaces the biome ID at an index using the same layout as {@link #getBiome(int)}.
     *
     * @param index layer index from 0 to 4095
     * @param biomeId ID to store without registry validation
     * @throws IndexOutOfBoundsException if the index is outside the layer
     */
    public void setBiome(int index, int biomeId) {
        Objects.checkIndex(index, SIZE);
        if (this.palette.getInt(this.paletteIndexAt(index)) == biomeId) {
            return;
        }

        int paletteId = this.idFor(biomeId);
        Objects.requireNonNull(this.bitArray).set(index, paletteId);
        this.needsCompaction = true;
    }

    /**
     * Returns an independently mutable copy of this layer.
     */
    public BiomeStorage copy() {
        return new BiomeStorage(
                this.bitArray == null ? null : this.bitArray.copy(),
                new IntArrayList(this.palette),
                this.needsCompaction
        );
    }

    /**
     * Writes runtime biome IDs, reusing the previous layer when safe for network decoding.
     * Singleton and leading-zero palettes are always written in full.
     *
     * @param buffer destination buffer at its current writer index
     * @param previous preceding encoded layer, or {@code null} for the first layer
     */
    public void writeToNetwork(ByteBuf buffer, @Nullable BiomeStorage previous) {
        this.compact();
        if (previous != null && this.bitArray != null && previous.bitArray != null
                && this.palette.getInt(0) != 0 && previous.palette.getInt(0) != 0
                && this.hasSameValues(previous)) {
            buffer.writeByte((COPY_PREVIOUS_BITS << 1) | 1);
            return;
        }

        this.writeIndices(buffer, true);
        if (this.bitArray != null) {
            VarInts.writeInt(buffer, this.palette.size());
        }

        for (int index = 0; index < this.palette.size(); index++) {
            VarInts.writeInt(buffer, this.palette.getInt(index));
        }
    }

    /**
     * Writes fixed-width biome IDs, reusing an identical previous layer when available.
     *
     * @param buffer destination buffer at its current writer index
     * @param previous preceding encoded layer, or {@code null} for the first layer
     */
    public void writeToDisk(ByteBuf buffer, @Nullable BiomeStorage previous) {
        this.compact();
        if (previous != null && this.hasSameValues(previous)) {
            buffer.writeByte(COPY_PREVIOUS_BITS << 1);
            return;
        }

        this.writeIndices(buffer, false);
        if (this.bitArray != null) {
            buffer.writeIntLE(this.palette.size());
        }

        for (int index = 0; index < this.palette.size(); index++) {
            buffer.writeIntLE(this.palette.getInt(index));
        }
    }

    /**
     * Reads one disk layer from the current buffer position, leaving following bytes unread.
     * A copy-last record returns an independent copy of the previous layer.
     * Invalid headers, palette sizes and indices are rejected.
     *
     * @param buffer source buffer at its current reader index
     * @param previous preceding decoded layer, or {@code null} for the first layer
     */
    public static BiomeStorage readFromDisk(ByteBuf buffer, @Nullable BiomeStorage previous) {
        int header = buffer.readUnsignedByte();
        int bits = header >>> 1;
        if (bits == COPY_PREVIOUS_BITS) {
            if (previous == null) {
                throw new IllegalArgumentException("Biome copy-last record has no previous layer");
            }

            return previous.copy();
        }

        // Biome IDs remain fixed-width on disk regardless of the header's low bit.
        if (bits == 0) {
            return new BiomeStorage(buffer.readIntLE());
        }

        BitArrayVersion version = BitArrayVersion.fromBits(bits);
        int[] words = new int[version.wordCount(SIZE)];
        for (int index = 0; index < words.length; index++) {
            words[index] = buffer.readIntLE();
        }

        int paletteSize = buffer.readIntLE();
        if (paletteSize < 1 || paletteSize - 1 > version.getMaxValue()) {
            throw new IllegalArgumentException("Invalid biome palette size " + paletteSize + " for " + bits + "-bit indices");
        }

        IntList palette = new IntArrayList(paletteSize);
        for (int index = 0; index < paletteSize; index++) {
            palette.add(buffer.readIntLE());
        }

        BiomeStorage storage = new BiomeStorage(version.createArray(SIZE, words), palette, true);
        storage.compact();
        return storage;
    }

    private int paletteIndexAt(int index) {
        return this.bitArray == null ? 0 : this.bitArray.get(index);
    }

    private int idFor(int biomeId) {
        int existing = this.paletteIndex.get(biomeId);
        if (existing != -1) {
            return existing;
        }

        if (this.palette.size() >= SIZE) {
            this.compact();
        }

        int index = this.palette.size();
        if (this.bitArray == null || index > this.bitArray.getVersion().getMaxValue()) {
            this.resize(BitArrayVersion.forPaletteSize(index + 1));
        }

        this.palette.add(biomeId);
        this.paletteIndex.put(biomeId, index);
        return index;
    }

    private void resize(BitArrayVersion version) {
        BitArray resized = version.createArray(SIZE);
        if (this.bitArray != null) {
            for (int index = 0; index < SIZE; index++) {
                resized.set(index, this.bitArray.get(index));
            }
        }

        this.bitArray = resized;
    }

    private void compact() {
        if (!this.needsCompaction) {
            return;
        }

        IntList livePalette = new IntArrayList();
        Int2IntOpenHashMap liveIndex = new Int2IntOpenHashMap();
        liveIndex.defaultReturnValue(-1);
        int[] remap = new int[this.palette.size()];
        Arrays.fill(remap, -1);

        for (int index = 0; index < SIZE; index++) {
            int oldId = this.paletteIndexAt(index);
            if (oldId >= this.palette.size()) {
                throw new IllegalArgumentException("Biome palette index " + oldId + " at position " + index + " exceeds palette size " + this.palette.size());
            }

            if (remap[oldId] == -1) {
                int biomeId = this.palette.getInt(oldId);
                int newId = liveIndex.get(biomeId);
                if (newId == -1) {
                    newId = livePalette.size();
                    livePalette.add(biomeId);
                    liveIndex.put(biomeId, newId);
                }
                remap[oldId] = newId;
            }
        }

        BitArray compacted = null;
        if (livePalette.size() > 1) {
            compacted = BitArrayVersion.forPaletteSize(livePalette.size()).createArray(SIZE);
            for (int index = 0; index < SIZE; index++) {
                compacted.set(index, remap[this.paletteIndexAt(index)]);
            }
        }

        this.palette.clear();
        this.palette.addAll(livePalette);
        this.rebuildPaletteIndex();
        this.bitArray = compacted;
        this.needsCompaction = false;
    }

    private boolean hasSameValues(BiomeStorage other) {
        if (this == other) {
            return true;
        }

        if (this.bitArray == null && other.bitArray == null) {
            return this.palette.getInt(0) == other.palette.getInt(0);
        }

        if (this.bitArray != null && other.bitArray != null
                && this.bitArray.getVersion() == other.bitArray.getVersion()
                && this.palette.equals(other.palette)
                && Arrays.equals(this.bitArray.getWords(), other.bitArray.getWords())) {
            return true;
        }

        for (int index = 0; index < SIZE; index++) {
            if (this.palette.getInt(this.paletteIndexAt(index)) != other.palette.getInt(other.paletteIndexAt(index))) {
                return false;
            }
        }

        return true;
    }

    private void rebuildPaletteIndex() {
        this.paletteIndex.clear();
        for (int index = 0; index < this.palette.size(); index++) {
            this.paletteIndex.put(this.palette.getInt(index), index);
        }
    }

    private void writeIndices(ByteBuf buffer, boolean runtime) {
        int bits = this.bitArray == null ? 0 : this.bitArray.getVersion().getBits();
        buffer.writeByte((bits << 1) | (runtime ? 1 : 0));
        if (this.bitArray != null) {
            for (int word : this.bitArray.getWords()) {
                buffer.writeIntLE(word);
            }
        }
    }
}
