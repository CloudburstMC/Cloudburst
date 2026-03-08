package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.ChunkSection;
import org.cloudburstmc.server.utils.NibbleArray;

import static com.google.common.base.Preconditions.checkElementIndex;

public class CloudChunkSection implements ChunkSection {

    public static final int DISK_CHUNK_SECTION_VERSION = 8;
    public static final int CHUNK_SECTION_VERSION = 9;
    public static final int SIZE = 4096;
    public static final int DEFAULT_BIOME_ID = 0;

    private final BlockStorage[] storage;
    private final NibbleArray blockLight;
    private final NibbleArray skyLight;
    private BiomeStorage biomeStorage;

    public CloudChunkSection() {
        this(new BlockStorage[]{new BlockStorage(), new BlockStorage()}, new NibbleArray(SIZE), new NibbleArray(SIZE), new BiomeStorage(DEFAULT_BIOME_ID));
    }

    public CloudChunkSection(BlockStorage[] blockStorage) {
        this(blockStorage, new NibbleArray(SIZE), new NibbleArray(SIZE), new BiomeStorage(DEFAULT_BIOME_ID));
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
        this.biomeStorage = new BiomeStorage(DEFAULT_BIOME_ID);
    }

    private CloudChunkSection(BlockStorage[] storage, NibbleArray blockLight, NibbleArray skyLight, BiomeStorage biomeStorage) {
        this.storage = storage;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
        this.biomeStorage = biomeStorage;
    }

    public static int blockIndex(int x, int y, int z) {
        return (x << 8) | (z << 4) | y;
    }

    public static void checkBounds(int x, int y, int z) {
        Preconditions.checkArgument(x >= 0 && x < 16, "x (%s) is not between 0 and 15", x);
        Preconditions.checkArgument(y >= 0 && y < 16, "y (%s) is not between 0 and 15", y);
        Preconditions.checkArgument(z >= 0 && z < 16, "z (%s) is not between 0 and 15", z);
    }

    private static void checkXZ(int x, int z) {
        Preconditions.checkArgument(x >= 0 && x < 16, "x (%s) is not between 0 and 15", x);
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

    /**
     * Writes this section to the network buffer in sub-chunk request mode.
     *
     * @param buffer   the buffer to write to
     * @param sectionY the absolute section Y index (e.g. -4 for Y=-64 to -49, 0 for Y=0 to 15)
     */
    public void writeToNetwork(ByteBuf buffer, int sectionY) {
        int layerCount = effectiveLayerCount();
        buffer.writeByte(CHUNK_SECTION_VERSION);
        buffer.writeByte(layerCount);
        buffer.writeByte(sectionY);
        for (int i = 0; i < layerCount; i++) {
            this.storage[i].writeToNetwork(buffer);
        }
    }

    /**
     * Writes this section to a buffer for on-disk serialization.
     * Uses version 8 format (no sectionY byte) so existing world saves remain compatible.
     * Layer 1 is omitted entirely when it is all-air and has not been modified since load.
     */
    public void writeToDisk(ByteBuf buffer) {
        for (BlockStorage blockStorage : this.storage) {
            if (blockStorage.isCompactNeeded()) {
                blockStorage.compact();
            }
        }
        int layerCount = effectiveLayerCount();
        buffer.writeByte(DISK_CHUNK_SECTION_VERSION);
        buffer.writeByte(layerCount);
        for (int i = 0; i < layerCount; i++) {
            this.storage[i].writeToStorage(buffer);
            this.storage[i].clearDirty();
        }
    }

    /**
     * Returns the number of layers to actually serialize. Trailing layers are excluded
     * when they are both all-air and unmodified since load, but the count is always at
     * least 1 (layer 0 is always present).
     */
    private int effectiveLayerCount() {
        int count = this.storage.length;
        while (count > 1 && this.storage[count - 1].isEmpty() && !this.storage[count - 1].isDirty()) {
            count--;
        }
        return count;
    }

    /**
     * Sets the biome ID for every Y position in a single XZ column within this section.
     *
     * @param x       0–15 within the section
     * @param z       0–15 within the section
     * @param biomeId raw biome integer ID
     */
    public void fillColumnBiome(int x, int z, int biomeId) {
        checkXZ(x, z);
        for (int y = 0; y < 16; y++) {
            this.biomeStorage.setBiome(blockIndex(x, y, z), biomeId);
        }
    }

    /**
     * Returns the biome ID at the given intra-section coordinates.
     *
     * @param x 0–15 within the section
     * @param y 0–15 within the section
     * @param z 0–15 within the section
     */
    public int getBiome(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.biomeStorage.getBiome(blockIndex(x, y, z));
    }

    /**
     * Sets the biome ID at the given intra-section coordinates.
     *
     * @param x       0–15 within the section
     * @param y       0–15 within the section
     * @param z       0–15 within the section
     * @param biomeId raw biome integer ID
     */
    public void setBiome(int x, int y, int z, int biomeId) {
        checkBounds(x, y, z);
        this.biomeStorage.setBiome(blockIndex(x, y, z), biomeId);
    }

    /**
     * Returns the underlying {@link BiomeStorage} for this section.
     */
    public BiomeStorage getBiomeStorage() {
        return this.biomeStorage;
    }

    /**
     * Replaces the underlying {@link BiomeStorage} for this section.
     */
    public void setBiomeStorage(BiomeStorage biomeStorage) {
        this.biomeStorage = biomeStorage;
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
        return new CloudChunkSection(storage, blockLight.copy(), skyLight.copy(), this.biomeStorage.copy());
    }
}
