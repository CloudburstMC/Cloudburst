package org.cloudburstmc.server.level.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.cloudburstmc.api.block.BlockStates.AIR;

/**
 * Stores one 16-by-16-by-16 block layer using identity-based palette entries.
 * A singleton palette has no index array. Instances are not thread-safe.
 * Buffer methods read or append one layer without releasing the supplied buffer.
 */
public class BlockStorage {

    private static final int SIZE = 4096;

    private final List<BlockState> palette;
    private final Reference2IntOpenHashMap<BlockState> paletteIndex;
    private @Nullable BitArray bitArray;
    private int nonAirCount;
    private boolean needsCompaction;

    /**
     * Creates a layer filled with air.
     */
    public BlockStorage() {
        this(null, new ReferenceArrayList<>(List.of(AIR)), 0, false);
    }

    private BlockStorage(@Nullable BitArray bitArray, List<BlockState> palette, int nonAirCount, boolean needsCompaction) {
        this.bitArray = bitArray;
        this.palette = palette;
        this.paletteIndex = new Reference2IntOpenHashMap<>(palette.size());
        this.paletteIndex.defaultReturnValue(-1);
        this.rebuildPaletteIndex();
        this.nonAirCount = nonAirCount;
        this.needsCompaction = needsCompaction;
    }

    /**
     * Returns the state at {@code (x << 8) | (z << 4) | y} within the layer.
     * Each local coordinate ranges from 0 to 15.
     *
     * @throws IndexOutOfBoundsException if the index is outside 0 to 4095
     */
    public BlockState getBlock(int index) {
        Objects.checkIndex(index, SIZE);
        return this.palette.get(this.paletteIndexAt(index));
    }

    /**
     * Replaces the state at an index using the same layout as {@link #getBlock(int)}.
     *
     * @param index layer index from 0 to 4095
     * @param state non-null state to store
     * @throws IndexOutOfBoundsException if the index is outside the layer
     */
    public void setBlock(int index, BlockState state) {
        Objects.checkIndex(index, SIZE);
        Objects.requireNonNull(state, "state");
        BlockState previous = this.palette.get(this.paletteIndexAt(index));
        if (previous == state) {
            return;
        }

        int paletteId = this.idFor(state);
        Objects.requireNonNull(this.bitArray).set(index, paletteId);
        if (previous == AIR) {
            this.nonAirCount++;
        } else if (state == AIR) {
            this.nonAirCount--;
        }

        this.needsCompaction = true;
    }

    /**
     * Returns whether every position contains {@link org.cloudburstmc.api.block.BlockStates#AIR}.
     */
    public boolean isEmpty() {
        return this.nonAirCount == 0;
    }

    /**
     * Returns an independently mutable copy sharing only the immutable block states.
     */
    public BlockStorage copy() {
        return new BlockStorage(
                this.bitArray == null ? null : this.bitArray.copy(),
                new ReferenceArrayList<>(this.palette),
                this.nonAirCount,
                this.needsCompaction
        );
    }

    /**
     * Appends a layer encoded with runtime block IDs.
     */
    public void writeToNetwork(ByteBuf buffer) {
        this.compact();
        this.writeIndices(buffer, true);

        if (this.bitArray != null) {
            VarInts.writeInt(buffer, this.palette.size());
        }

        for (BlockState state : this.palette) {
            VarInts.writeInt(buffer, BlockPalette.INSTANCE.getDefinition(state).getRuntimeId());
        }
    }

    /**
     * Appends a layer encoded with named block states for persistence.
     */
    public void writeToStorage(ByteBuf buffer) {
        this.compact();
        this.writeIndices(buffer, false);

        if (this.bitArray != null) {
            buffer.writeIntLE(this.palette.size());
        }

        try (NBTOutputStream output = NbtUtils.createWriterLE(new ByteBufOutputStream(buffer))) {
            for (BlockState state : this.palette) {
                output.writeTag(BlockStateCodec.write(state));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to write block palette", exception);
        }
    }

    /**
     * Reads one named-state layer from the current buffer position.
     * Bytes following the layer remain unread.
     * Runtime-ID palettes, invalid indices and unknown states are rejected.
     */
    public static BlockStorage readFromStorage(ByteBuf buffer) {
        return readFromStorage(buffer, BlockStateCodec::read);
    }

    /**
     * Decodes a named-state layer with the supplied palette-entry decoder.
     * Storage headers and indices are validated independently of state resolution.
     * Each decoder result must be non-null.
     * Bytes following the layer remain unread.
     */
    public static BlockStorage readFromStorage(ByteBuf buffer, Function<NbtMap, BlockState> decoder) {
        Objects.requireNonNull(decoder, "decoder");
        int header = buffer.readUnsignedByte();
        if ((header & 1) != 0) {
            throw new IllegalArgumentException("Runtime-ID palettes cannot be read as persisted block states");
        }

        int bits = header >>> 1;
        BitArray indices = null;
        int paletteSize = 1;
        if (bits != 0) {
            BitArrayVersion version = BitArrayVersion.fromBits(bits);
            int[] words = new int[version.wordCount(SIZE)];
            for (int index = 0; index < words.length; index++) {
                words[index] = buffer.readIntLE();
            }

            indices = version.createArray(SIZE, words);
            paletteSize = buffer.readIntLE();
            if (paletteSize < 1 || paletteSize - 1 > version.getMaxValue()) {
                throw new IllegalArgumentException("Invalid block palette size " + paletteSize + " for " + bits + "-bit indices");
            }
        }

        List<BlockState> palette = new ReferenceArrayList<>(paletteSize);
        try (NBTInputStream input = NbtUtils.createReaderLE(new ByteBufInputStream(buffer))) {
            for (int index = 0; index < paletteSize; index++) {
                if (!(input.readTag() instanceof NbtMap tag)) {
                    throw new IllegalArgumentException("Block palette entry " + index + " must be a compound");
                }

                palette.add(Objects.requireNonNull(decoder.apply(tag), "decoded block state"));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read block palette", exception);
        }

        BlockStorage storage = new BlockStorage(indices, palette, 0, true);
        storage.compact();
        return storage;
    }

    private int paletteIndexAt(int index) {
        return this.bitArray == null ? 0 : this.bitArray.get(index);
    }

    private int idFor(BlockState state) {
        int existing = this.paletteIndex.getInt(state);
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

        this.palette.add(state);
        this.paletteIndex.put(state, index);
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

        List<BlockState> livePalette = new ReferenceArrayList<>();
        Reference2IntOpenHashMap<BlockState> liveIndex = new Reference2IntOpenHashMap<>();
        liveIndex.defaultReturnValue(-1);
        int[] remap = new int[this.palette.size()];
        Arrays.fill(remap, -1);
        int nonAir = 0;

        for (int index = 0; index < SIZE; index++) {
            int oldId = this.paletteIndexAt(index);
            if (oldId >= this.palette.size()) {
                throw new IllegalArgumentException("Block palette index " + oldId + " at position " + index + " exceeds palette size " + this.palette.size());
            }

            BlockState state = this.palette.get(oldId);
            if (remap[oldId] == -1) {
                int newId = liveIndex.getInt(state);
                if (newId == -1) {
                    newId = livePalette.size();
                    livePalette.add(state);
                    liveIndex.put(state, newId);
                }
                remap[oldId] = newId;
            }

            if (state != AIR) {
                nonAir++;
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
        this.nonAirCount = nonAir;
        this.needsCompaction = false;
    }

    private void rebuildPaletteIndex() {
        this.paletteIndex.clear();
        for (int index = 0; index < this.palette.size(); index++) {
            this.paletteIndex.put(this.palette.get(index), index);
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
