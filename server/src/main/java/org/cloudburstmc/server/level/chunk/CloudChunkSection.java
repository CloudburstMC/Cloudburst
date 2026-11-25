package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.level.chunk.ChunkSection;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.utils.NibbleArray;

public class CloudChunkSection implements ChunkSection {

    public static final int DISK_CHUNK_SECTION_VERSION = 8;
    public static final int CHUNK_SECTION_VERSION = 9;
    public static final int SIZE = 4096;
    public static final int DEFAULT_BIOME_ID = 0;

    private final BlockRegistry blockRegistry;
    private final BlockStorage[] storage;
    private final NibbleArray blockLight;
    private final NibbleArray skyLight;

    @Getter
    @Setter
    private BiomeStorage biomeStorage;
    private int tickingBlockCount;

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

    public BlockState getBlockState(int x, int y, int z, BlockLayer layer) {
        checkBounds(x, y, z);
        return this.storage[BlockLayerStorage.index(layer)].getBlock(blockIndex(x, y, z));
    }

    public BlockState setBlockState(int x, int y, int z, BlockLayer layer, BlockState blockState) {
        checkBounds(x, y, z);
        int storageIndex = BlockLayerStorage.index(layer);
        int idx = blockIndex(x, y, z);

        BlockState oldState = this.storage[storageIndex].getBlock(idx);
        int tickDelta = 0;
        if (layer == BlockLayer.PRIMARY && oldState.getType() != blockState.getType()) {
            int previous = this.canRandomTick(oldState) ? 1 : 0;
            int next = this.canRandomTick(blockState) ? 1 : 0;
            tickDelta = next - previous;
        }

        this.storage[storageIndex].setBlock(idx, blockState);
        this.tickingBlockCount += tickDelta;
        return oldState;
    }

    /**
     * Returns {@code true} when this section contains at least one block
     * that has the {@code CAN_RANDOM_TICK} component set to {@code true}.
     */
    public boolean isRandomlyTicking() {
        return this.tickingBlockCount > 0;
    }

    /**
     * Returns the current primary state if it can randomly tick, or {@code null} otherwise.
     */
    public @Nullable BlockState getRandomTickingState(int x, int y, int z) {
        BlockState state = this.getBlockState(x, y, z, BlockLayer.PRIMARY);
        return this.canRandomTick(state) ? state : null;
    }

    private boolean canRandomTick(BlockState state) {
        if (state == BlockStates.AIR) {
            return false;
        }

        return Preconditions.checkNotNull(
                this.blockRegistry.requireComponent(state.getType(), BlockComponents.CAN_RANDOM_TICK),
                "Random tick component is not registered for %s", state.getType()
        );
    }

    private void recalcTickingBlocks() {
        int count = 0;
        for (int index = 0; index < SIZE; index++) {
            if (this.canRandomTick(this.storage[0].getBlock(index))) {
                count++;
            }
        }
        this.tickingBlockCount = count;
    }

    public int getSkyLight(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.skyLight.get(blockIndex(x, y, z));
    }

    public void setSkyLight(int x, int y, int z, int val) {
        checkBounds(x, y, z);
        Preconditions.checkArgument(val >= 0 && val <= 15, "sky light (%s) is not between 0 and 15", val);
        this.skyLight.set(blockIndex(x, y, z), (byte) val);
    }

    public int getBlockLight(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.blockLight.get(blockIndex(x, y, z));
    }

    public void setBlockLight(int x, int y, int z, int val) {
        checkBounds(x, y, z);
        Preconditions.checkArgument(val >= 0 && val <= 15, "block light (%s) is not between 0 and 15", val);
        this.blockLight.set(blockIndex(x, y, z), (byte) val);
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
     * Uses version 8 format without a section Y byte and omits trailing empty layers.
     */
    public void writeToDisk(ByteBuf buffer) {
        int layerCount = effectiveLayerCount();
        buffer.writeByte(DISK_CHUNK_SECTION_VERSION);
        buffer.writeByte(layerCount);
        for (int i = 0; i < layerCount; i++) {
            this.storage[i].writeToStorage(buffer);
        }
    }

    /**
     * Includes the primary layer and any layers through the last nonempty layer.
     */
    private int effectiveLayerCount() {
        int count = this.storage.length;
        while (count > 1 && this.storage[count - 1].isEmpty()) {
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
}
