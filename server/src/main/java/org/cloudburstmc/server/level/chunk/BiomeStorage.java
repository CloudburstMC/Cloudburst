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
 */
public class BiomeStorage {

    private static final int SIZE = 4096;
    private static final int COPY_PREVIOUS_DISK = 0xFE;
    private static final int COPY_PREVIOUS_NETWORK = 0xFF;

    private final IntList palette;
    private final Int2IntOpenHashMap paletteIndex;
    private @Nullable BitArray bitArray;
    private boolean needsCompaction;

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
     */
    public int getBiome(int index) {
        Objects.checkIndex(index, SIZE);
        return this.palette.getInt(this.paletteIndexAt(index));
    }

    public void setBiome(int index, int biomeId) {
        Objects.checkIndex(index, SIZE);
        if (this.palette.getInt(this.paletteIndexAt(index)) == biomeId) {
            return;
        }

        int paletteId = this.idFor(biomeId);
        Objects.requireNonNull(this.bitArray).set(index, paletteId);
        this.needsCompaction = true;
    }

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
     */
    public void writeToNetwork(ByteBuf buffer, @Nullable BiomeStorage previous) {
        this.compact();
        if (previous != null && this.bitArray != null && previous.bitArray != null
                && this.palette.getInt(0) != 0 && previous.palette.getInt(0) != 0
                && this.hasSameValues(previous)) {
            buffer.writeByte(COPY_PREVIOUS_NETWORK);
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
     */
    public void writeToDisk(ByteBuf buffer, @Nullable BiomeStorage previous) {
        this.compact();
        if (previous != null && this.hasSameValues(previous)) {
            buffer.writeByte(COPY_PREVIOUS_DISK);
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
     * Decodes a complete disk layer. A copy-last record returns an independent copy.
     * Invalid headers, palette sizes and indices are rejected.
     */
    public static BiomeStorage readFromDisk(ByteBuf buffer, @Nullable BiomeStorage previous) {
        int header = buffer.readUnsignedByte();
        if (header == COPY_PREVIOUS_DISK) {
            if (previous == null) {
                throw new IllegalArgumentException("Biome copy-last record has no previous layer");
            }

            return previous.copy();
        }

        if ((header & 1) != 0) {
            throw new IllegalArgumentException("Network biome palettes cannot be read as persisted biome data");
        }

        int bits = header >>> 1;
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
