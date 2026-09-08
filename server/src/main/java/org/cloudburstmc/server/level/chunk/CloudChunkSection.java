package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.level.chunk.ChunkSection;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.utils.NibbleArray;

import static com.google.common.base.Preconditions.checkElementIndex;

public class CloudChunkSection implements ChunkSection {

    public static final int DISK_CHUNK_SECTION_VERSION = 8;
    public static final int CHUNK_SECTION_VERSION = 9;
    public static final int SIZE = 4096;
    public static final int DEFAULT_BIOME_ID = 0;

    private final BlockRegistry blockRegistry;
    private final BlockStorage[] storage;
    private final NibbleArray blockLight;
    private final NibbleArray skyLight;
    /**
     * Compact position index of all randomly-ticking blocks in this section.
     * Kept in sync with {@link #tickingBlockCount} by {@link #setBlockState}.
     * Used by the random-tick loop for O(1) rejection of non-ticking rolls.
     */
    private final SectionTickList tickingList;
    private BiomeStorage biomeStorage;
    /**
     * Number of blocks in layer 0 of this section that have the
     * {@code CAN_RANDOM_TICK} component set to {@code true}.
     * Maintained incrementally in {@link #setBlockState}.
     */
    private short tickingBlockCount;

    public CloudChunkSection(BlockRegistry blockRegistry) {
        this(
                blockRegistry,
                new BlockStorage[]{new BlockStorage(), new BlockStorage()},
                new NibbleArray(SIZE),
                new NibbleArray(SIZE),
                new BiomeStorage(DEFAULT_BIOME_ID)
        );
    }

    public CloudChunkSection(BlockRegistry blockRegistry, BlockStorage[] blockStorage) {
        this(
                blockRegistry,
                blockStorage,
                new NibbleArray(SIZE),
                new NibbleArray(SIZE),
                new BiomeStorage(DEFAULT_BIOME_ID)
        );
    }

    public CloudChunkSection(BlockRegistry blockRegistry, BlockStorage[] storage, byte[] blockLight, byte[] skyLight) {
        this.blockRegistry = Preconditions.checkNotNull(blockRegistry, "blockRegistry");
        Preconditions.checkNotNull(storage, "storage");
        Preconditions.checkArgument(storage.length > 1, "Block storage length must be at least 2");
        for (BlockStorage blockStorage : storage) {
            Preconditions.checkNotNull(blockStorage, "storage");
        }

        this.storage = storage;
        this.blockLight = new NibbleArray(blockLight);
        this.skyLight = new NibbleArray(skyLight);
        this.biomeStorage = new BiomeStorage(DEFAULT_BIOME_ID);
        this.tickingList = new SectionTickList();
        this.tickingBlockCount = 0;
        recalcTickingBlocks();
    }

    private CloudChunkSection(
            BlockRegistry blockRegistry,
            BlockStorage[] storage,
            NibbleArray blockLight,
            NibbleArray skyLight,
            BiomeStorage biomeStorage
    ) {
        this.blockRegistry = Preconditions.checkNotNull(blockRegistry, "blockRegistry");
        this.storage = storage;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
        this.biomeStorage = biomeStorage;
        this.tickingList = new SectionTickList();
        this.tickingBlockCount = 0;
        recalcTickingBlocks();
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

    private boolean canRandomTick(BlockState state) {
        if (state == BlockStates.AIR) {
            return false;
        }

        return Preconditions.checkNotNull(
                this.blockRegistry.requireComponent(state.getType(), BlockComponents.CAN_RANDOM_TICK),
                "Random tick component is not registered for %s", state.getType());
    }

    void checkLayer(int layer) {
        checkElementIndex(layer, this.storage.length, "Invalid block layer");
    }

    public BlockState getBlockState(int x, int y, int z, int layer) {
        checkBounds(x, y, z);
        checkLayer(layer);
        return this.storage[layer].getBlock(blockIndex(x, y, z));
    }

    /**
     * Sets the block at the given intra-section coordinates and maintains the
     * {@link #tickingBlockCount} counter and {@link #tickingList} index
     * incrementally for layer 0 only.
     */
    public BlockState setBlockState(int x, int y, int z, int layer, BlockState blockState) {
        checkBounds(x, y, z);
        checkLayer(layer);
        int idx = blockIndex(x, y, z);

        BlockState oldState = this.storage[layer].getBlock(idx);
        if (layer == 0) {
            BlockState old = oldState;
            boolean oldTicks = canRandomTick(old);
            boolean newTicks = canRandomTick(blockState);

            if (oldTicks && !newTicks) {
                tickingBlockCount--;
                tickingList.remove(x, y, z);
            } else if (!oldTicks && newTicks) {
                tickingBlockCount++;
                tickingList.add(x, y, z, blockState);
            } else if (oldTicks) {
                tickingList.remove(x, y, z);
                tickingList.add(x, y, z, blockState);
            }
        }

        this.storage[layer].setBlock(idx, blockState);
        return oldState;
    }

    /**
     * Returns {@code true} when this section contains at least one block
     * that has the {@code CAN_RANDOM_TICK} component set to {@code true}.
     */
    public boolean isRandomlyTicking() {
        return tickingBlockCount > 0;
    }

    /**
     * Returns the compact position index of all randomly-ticking blocks in
     * this section. Use {@link SectionTickList#size()} as the modulus for the
     * probability-rejection loop; never iterate it when
     * {@link #isRandomlyTicking()} is {@code false}.
     */
    public SectionTickList getTickingList() {
        return tickingList;
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
     * @param sectionY the absolute section Y index
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
     */
    public void fillColumnBiome(int x, int z, int biomeId) {
        checkXZ(x, z);
        for (int y = 0; y < 16; y++) {
            this.biomeStorage.setBiome(blockIndex(x, y, z), biomeId);
        }
    }

    /**
     * Returns the biome ID at the given intra-section coordinates.
     */
    public int getBiome(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.biomeStorage.getBiome(blockIndex(x, y, z));
    }

    /**
     * Sets the biome ID at the given intra-section coordinates.
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

    /**
     * Returns {@code true} when all block storage layers are empty.
     */
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
        return new CloudChunkSection(
                this.blockRegistry,
                storage,
                blockLight.copy(),
                skyLight.copy(),
                this.biomeStorage.copy()
        );
    }

    /**
     * Rebuilds {@link #tickingBlockCount} and {@link #tickingList} from
     * scratch by scanning layer 0.
     */
    private void recalcTickingBlocks() {
        tickingList.clear();
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    BlockState state = this.storage[0].getBlock(blockIndex(x, y, z));
                    if (canRandomTick(state)) {
                        tickingList.add(x, y, z, state);
                        count++;
                    }
                }
            }
        }
        tickingBlockCount = (short) count;
    }
}
