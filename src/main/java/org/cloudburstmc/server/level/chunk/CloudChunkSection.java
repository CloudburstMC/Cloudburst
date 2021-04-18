package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.ChunkSection;
import org.cloudburstmc.server.utils.NibbleArray;

import static com.google.common.base.Preconditions.checkElementIndex;

public class CloudChunkSection implements ChunkSection {

    public static final int CHUNK_SECTION_VERSION = 8;
    public static final int SIZE = 4096;

    private final BlockStorage[] storage;
    private final NibbleArray blockLight;
    private final NibbleArray skyLight;

    public CloudChunkSection() {
        this(new BlockStorage[]{new BlockStorage(), new BlockStorage()}, new NibbleArray(SIZE),
                new NibbleArray(SIZE));
    }

    public CloudChunkSection(BlockStorage[] blockStorage) {
        this(blockStorage, new NibbleArray(SIZE), new NibbleArray(SIZE));
    }

    public CloudChunkSection(BlockStorage[] storage, byte[] blockLight, byte[] skyLight) {
        Preconditions.checkNotNull(storage, "storage");
        Preconditions.checkArgument(storage.length > 1, "Block storage length must be at least 2");
        for (BlockStorage blockStorage : storage) {
            Preconditions.checkNotNull(blockStorage, "storage");
        }

        this.storage = storage;
        this.blockLight = new NibbleArray(blockLight);
        this.skyLight = new NibbleArray(skyLight);
    }

    private CloudChunkSection(BlockStorage[] storage, NibbleArray blockLight, NibbleArray skyLight) {
        this.storage = storage;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

    public static int blockIndex(int x, int y, int z) {
        return (x << 8) | (z << 4) | y;
    }

    public static void checkBounds(int x, int y, int z) {
        Preconditions.checkArgument(x >= 0 && x < 16, "x (%s) is not between 0 and 15", x);
        Preconditions.checkArgument(y >= 0 && y < 16, "y (%s) is not between 0 and 15", y);
        Preconditions.checkArgument(z >= 0 && z < 16, "z (%s) is not between 0 and 15", z);
    }

    void checkLayer(int layer) {
        checkElementIndex(layer, this.storage.length, "Invalid block layer");
    }

    public BlockState getBlock(int x, int y, int z, int layer) {
        checkBounds(x, y, z);
        checkLayer(layer);
        return this.storage[layer].getBlock(blockIndex(x, y, z));
    }

    public void setBlock(int x, int y, int z, int layer, BlockState blockState) {
        checkBounds(x, y, z);
        checkLayer(layer);
        this.storage[layer].setBlock(blockIndex(x, y, z), blockState);
    }

    public byte getSkyLight(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.skyLight.get(blockIndex(x, y, z));
    }

    public void setSkyLight(int x, int y, int z, byte val) {
        checkBounds(x, y, z);
        this.skyLight.set(blockIndex(x, y, z), val);
    }

    public byte getBlockLight(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.blockLight.get(blockIndex(x, y, z));
    }

    public void setBlockLight(int x, int y, int z, byte val) {
        checkBounds(x, y, z);
        this.blockLight.set(blockIndex(x, y, z), val);
    }

    public void writeToNetwork(ByteBuf buffer) {
        buffer.writeByte(CHUNK_SECTION_VERSION);
        buffer.writeByte(this.storage.length);
        for (BlockStorage blockStorage : this.storage) {
            blockStorage.writeToNetwork(buffer);
        }
    }

    public NibbleArray getSkyLightArray() {
        return skyLight;
    }

    public NibbleArray getBlockLightArray() {
        return blockLight;
    }

    public BlockStorage[] getBlockStorageArray() {
        return storage;
    }

    public boolean isEmpty() {
        for (BlockStorage blockStorage : this.storage) {
            if (!blockStorage.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public CloudChunkSection copy() {
        BlockStorage[] storage = new BlockStorage[this.storage.length];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = this.storage[i].copy();
        }
        return new CloudChunkSection(storage, skyLight.copy(), blockLight.copy());
    }
}
