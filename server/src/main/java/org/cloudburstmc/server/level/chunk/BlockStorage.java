package org.cloudburstmc.server.level.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.cloudburstmc.api.block.BlockStates.AIR;

@Log4j2
public class BlockStorage {

    private static final int SIZE = 4096;
    private final List<BlockState> palette;
    private final Reference2IntOpenHashMap<BlockState> paletteIndex;
    private BitArray bitArray;

    public BlockStorage() {
        this(BitArrayVersion.V1);
    }

    public BlockStorage(BitArrayVersion version) {
        this.bitArray = version.createPalette(SIZE);
        this.palette = new ReferenceArrayList<>(16);
        this.paletteIndex = new Reference2IntOpenHashMap<>(16);
        this.paletteIndex.defaultReturnValue(-1);
        this.palette.add(AIR);
        this.paletteIndex.put(AIR, 0);
    }

    private BlockStorage(BitArray bitArray, List<BlockState> palette) {
        this.palette = palette;
        this.bitArray = bitArray;
        this.paletteIndex = new Reference2IntOpenHashMap<>(palette.size());
        this.paletteIndex.defaultReturnValue(-1);
        for (int i = 0; i < palette.size(); i++) {
            this.paletteIndex.put(palette.get(i), i);
        }
    }

    private static BitArrayVersion getVersionFromHeader(byte header) {
        return BitArrayVersion.get(header >> 1, true);
    }

    private int getPaletteHeader(BitArrayVersion version, boolean runtime) {
        return (version.getId() << 1) | (runtime ? 1 : 0);
    }

    public BlockState getBlock(int index) {
        return this.blockFor(this.bitArray.get(index));
    }

    public void setBlock(int index, BlockState blockState) {
        try {
            int idx = this.idFor(blockState);
            this.bitArray.set(index, idx);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to set block: " + blockState + ", palette: " + palette, e);
        }
    }

    public void writeToNetwork(ByteBuf buffer) {
        if (isEmpty()) {
            buffer.writeByte(0x01);
            VarInts.writeInt(buffer, CloudBlockRegistry.REGISTRY.getRuntimeId(AIR));
            return;
        }

        buffer.writeByte(getPaletteHeader(bitArray.getVersion(), true));

        for (int word : bitArray.getWords()) {
            buffer.writeIntLE(word);
        }

        VarInts.writeInt(buffer, palette.size());

        CloudBlockRegistry registry = CloudBlockRegistry.REGISTRY;
        palette.forEach(state -> VarInts.writeInt(buffer, registry.getRuntimeId(state)));
    }

    public void writeToStorage(ByteBuf buffer) {
        if (isEmpty()) {
            buffer.writeByte(0x00);
            try (ByteBufOutputStream stream = new ByteBufOutputStream(buffer);
                 NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
                nbtOutputStream.writeTag(BlockPalette.INSTANCE.getSerialized(AIR));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        buffer.writeByte(getPaletteHeader(bitArray.getVersion(), false));
        for (int word : bitArray.getWords()) {
            buffer.writeIntLE(word);
        }

        buffer.writeIntLE(this.palette.size());

        try (ByteBufOutputStream stream = new ByteBufOutputStream(buffer);
             NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
            for (BlockState state : this.palette) {
                nbtOutputStream.writeTag(BlockPalette.INSTANCE.getSerialized(state));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readFromStorage(ByteBuf buffer) {
        byte headerByte = buffer.readByte();
        int bitsPerEntry = (headerByte & 0xFF) >> 1;

        if (bitsPerEntry == 0) {
            this.palette.clear();
            this.paletteIndex.clear();
            this.bitArray = BitArrayVersion.V1.createPalette(SIZE);
            try (ByteBufInputStream stream = new ByteBufInputStream(buffer);
                 NBTInputStream nbtInputStream = NbtUtils.createReaderLE(stream)) {
                try {
                    NbtMap tag = (NbtMap) nbtInputStream.readTag();
                    BlockState state = CloudBlockRegistry.REGISTRY.getBlock(tag);
                    this.palette.add(state);
                    this.paletteIndex.put(state, 0);
                } catch (Exception e) {
                    log.throwing(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        BitArrayVersion version = getVersionFromHeader(headerByte);

        int expectedWordCount = version.getWordsForSize(SIZE);
        int[] words = new int[expectedWordCount];
        for (int i = 0; i < expectedWordCount; i++) {
            words[i] = buffer.readIntLE();
        }
        this.bitArray = version.createPalette(SIZE, words);

        this.palette.clear();
        this.paletteIndex.clear();
        int paletteSize = buffer.readIntLE();

        checkArgument(version.getMaxEntryValue() >= paletteSize - 1,
                "Palette is too large. Max size %s. Actual size %s", version.getMaxEntryValue(),
                paletteSize);

        try (ByteBufInputStream stream = new ByteBufInputStream(buffer);
             NBTInputStream nbtInputStream = NbtUtils.createReaderLE(stream)) {
            for (int i = 0; i < paletteSize; i++) {
                try {
                    NbtMap tag = (NbtMap) nbtInputStream.readTag();
                    BlockState state = CloudBlockRegistry.REGISTRY.getBlock(tag);

                    if (this.paletteIndex.containsKey(state)) {
                        log.warn("Palette contains block state ({}) twice! ({}) (palette: {})", state, tag, this.palette);
                    }

                    this.paletteIndex.put(state, this.palette.size());
                    this.palette.add(state);
                } catch (Exception e) {
                    log.throwing(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onResize(BitArrayVersion version) {
        BitArray newBitArray = version.createPalette(SIZE);

        for (int i = 0; i < SIZE; i++) {
            newBitArray.set(i, this.bitArray.get(i));
        }
        this.bitArray = newBitArray;
    }

    private int idFor(BlockState blockState) {
        int index = this.paletteIndex.getInt(blockState);
        if (index != -1) {
            return index;
        }

        index = this.palette.size();
        BitArrayVersion version = this.bitArray.getVersion();
        if (index > version.getMaxEntryValue()) {
            BitArrayVersion next = version.next();
            if (next != null) {
                this.onResize(next);
            }
        }
        this.palette.add(blockState);
        this.paletteIndex.put(blockState, index);
        return index;
    }

    private BlockState blockFor(int index) {
        return this.palette.get(index);
    }

    public boolean isEmpty() {
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

    /**
     * Compacts this storage by removing unreferenced palette entries and downsizing
     * the bit-array to the smallest version that can hold the remaining entries.
     * AIR is always retained at index 0.
     */
    public void compact() {
        List<BlockState> newPalette = new ReferenceArrayList<>(this.palette.size());
        newPalette.add(AIR);

        // Maps old palette index to new palette index.
        int[] indexMap = new int[this.palette.size()];

        // Scan every block slot and collect live palette entries.
        for (int i = 0; i < SIZE; i++) {
            int oldIdx = this.bitArray.get(i);
            if (indexMap[oldIdx] == 0 && oldIdx != 0) {
                // Not yet mapped; assign next slot in the new palette.
                BlockState state = this.palette.get(oldIdx);
                int newIdx = newPalette.size();
                newPalette.add(state);
                indexMap[oldIdx] = newIdx;
            }
        }

        // Pick the smallest BitArrayVersion that accommodates newPalette.size() entries.
        int liveCount = newPalette.size();
        BitArrayVersion minVersion = BitArrayVersion.getMinimalVersion(liveCount);

        // Re-index all blocks into the new bit-array.
        BitArray newBitArray = minVersion.createPalette(SIZE);
        for (int i = 0; i < SIZE; i++) {
            newBitArray.set(i, indexMap[this.bitArray.get(i)]);
        }

        this.palette.clear();
        this.palette.addAll(newPalette);
        this.bitArray = newBitArray;
        this.paletteIndex.clear();
        for (int i = 0; i < this.palette.size(); i++) {
            this.paletteIndex.put(this.palette.get(i), i);
        }
    }

    public BlockStorage copy() {
        return new BlockStorage(this.bitArray.copy(), new ReferenceArrayList<>(this.palette));
    }
}
