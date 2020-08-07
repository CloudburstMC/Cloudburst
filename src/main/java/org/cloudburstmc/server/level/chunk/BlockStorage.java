package org.cloudburstmc.server.level.chunk;

import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.*;
import com.nukkitx.network.VarInts;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntConsumer;

import static com.google.common.base.Preconditions.checkArgument;

public class BlockStorage {

    private static final int SIZE = 4096;

    private final IntList palette;
    private BitArray bitArray;

    public BlockStorage() {
        this(BitArrayVersion.V2);
    }

    public BlockStorage(BitArrayVersion version) {
        this.bitArray = version.createPalette(SIZE);
        this.palette = new IntArrayList(16);
        this.palette.add(0); // Air is at the start of every palette.
    }

    private BlockStorage(BitArray bitArray, IntList palette) {
        this.palette = palette;
        this.bitArray = bitArray;
    }

    private int getPaletteHeader(BitArrayVersion version, boolean runtime) {
        return (version.getId() << 1) | (runtime ? 1 : 0);
    }

    private static BitArrayVersion getVersionFromHeader(byte header) {
        return BitArrayVersion.get(header >> 1, true);
    }

    public BlockState getBlock(int index) {
        return this.blockFor(this.bitArray.get(index));
    }

    public void setBlock(int index, BlockState blockState) {
        try {
            int idx = this.idFor(BlockRegistry.get().getRuntimeId(blockState));
            this.bitArray.set(index, idx);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to set block: " + blockState + ", palette: " + palette, e);
        }
    }

    public void writeToNetwork(ByteBuf buffer) {
        buffer.writeByte(getPaletteHeader(bitArray.getVersion(), true));

        for (int word : bitArray.getWords()) {
            buffer.writeIntLE(word);
        }

        VarInts.writeInt(buffer, palette.size());
        palette.forEach((IntConsumer) id -> VarInts.writeInt(buffer, id));
    }

    public void writeToStorage(ByteBuf buffer) {
        buffer.writeByte(getPaletteHeader(bitArray.getVersion(), false));
        for (int word : bitArray.getWords()) {
            buffer.writeIntLE(word);
        }

        buffer.writeIntLE(this.palette.size());

        try (ByteBufOutputStream stream = new ByteBufOutputStream(buffer);
             NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
            for (int runtimeId : palette.toIntArray()) {
                BlockState blockState = BlockRegistry.get().getBlock(runtimeId);

                nbtOutputStream.writeTag(BlockPalette.INSTANCE.getSerialized(blockState));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void readFromStorage(ByteBuf buffer) {
        BitArrayVersion version = getVersionFromHeader(buffer.readByte());

        int expectedWordCount = version.getWordsForSize(SIZE);
        int[] words = new int[expectedWordCount];
        for (int i = 0; i < expectedWordCount; i++) {
            words[i] = buffer.readIntLE();
        }
        this.bitArray = version.createPalette(SIZE, words);

        this.palette.clear();
        int paletteSize = buffer.readIntLE();

        checkArgument(version.getMaxEntryValue() >= paletteSize - 1,
                "Palette is too large. Max size %s. Actual size %s", version.getMaxEntryValue(),
                paletteSize);

        try (ByteBufInputStream stream = new ByteBufInputStream(buffer);
             NBTInputStream nbtInputStream = NbtUtils.createReaderLE(stream)) {
            Map<NbtMap, BlockState> tags = new LinkedHashMap<>();
            for (int i = 0; i < paletteSize; i++) {
                NbtMap tag = (NbtMap) nbtInputStream.readTag();
                Identifier id = Identifier.fromString(tag.getString("name"));
                BlockState state = null;

                if (tag.containsKey("states", NbtType.COMPOUND)) {
                    state = BlockPalette.INSTANCE.getBlockState(tag);
                }
                if (state == null) {
                    tag = BlockStateUpdaters.updateBlockState(tag, tag.getInt("version"));
                    state = BlockPalette.INSTANCE.getBlockState(tag);
                }
                if (state == null) throw new IllegalStateException("Invalid block state\n" + tag);

                int runtimeId = BlockRegistry.get().getRuntimeId(state);
                checkArgument(!this.palette.contains(runtimeId),
                        "Palette contains block state (%s) twice! (%s) (palette: %s)", state, tags, this.palette);
                this.palette.add(runtimeId);
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

    private int idFor(int runtimeId) {
        int index = this.palette.indexOf(runtimeId);
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
        this.palette.add(runtimeId);
        return index;
    }

    private BlockState blockFor(int index) {
        int runtimeId = this.palette.getInt(index);
        return BlockRegistry.get().getBlock(runtimeId);
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

    public BlockStorage copy() {
        return new BlockStorage(this.bitArray.copy(), new IntArrayList(this.palette));
    }
}
