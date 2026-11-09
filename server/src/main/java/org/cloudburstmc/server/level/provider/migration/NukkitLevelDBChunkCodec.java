package org.cloudburstmc.server.level.provider.migration;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;

import java.io.IOException;

/**
 * Applies the pinned numeric mappings and Nukkit finalization rules during import.
 */
public class NukkitLevelDBChunkCodec implements LevelDBChunkCodec {

    private final NukkitBlockStateConverter blocks;
    private final NukkitItemDataConverter items;
    private final NukkitBlockStorageReader storage;

    public NukkitLevelDBChunkCodec() throws IOException {
        this.blocks = new NukkitBlockStateConverter();
        this.items = new NukkitItemDataConverter();
        this.storage = new NukkitBlockStorageReader(this.blocks);
    }

    @Override
    public BlockStorage[] readSection(byte[] data, int sectionY) throws IOException {
        return this.storage.readSection(data, sectionY);
    }

    @Override
    public BlockState readLegacyBlock(int id, int metadata) {
        return this.blocks.read(id, metadata);
    }

    @Override
    public BlockState readTickState(NbtMap tag) throws IOException {
        NbtMap state = tag.getCompound("blockState");
        if (state.containsKey("name")) {
            return this.blocks.readNamed(state);
        }

        if (tag.containsKey("tileID", NbtType.BYTE)) {
            return this.blocks.read(tag.getByte("tileID") & 0xff, 0);
        }

        throw new IOException("Pending tick has no block reference");
    }

    @Override
    public int readColumnBiome(byte[] biomes, int x, int z) {
        return biomes[x * 16 + z] & 0xff;
    }

    @Override
    public int readHeight(int encoded, int minHeight, boolean threeDimensional) {
        return encoded;
    }

    @Override
    public ChunkGenerationStatus readGeneration(byte @Nullable [] data) throws IOException {
        return NukkitChunkGeneration.readLevelDB(data);
    }

    @Override
    public NbtMap convertEntity(NbtMap tag) {
        return this.blocks.convertEntity(this.items.convert(tag));
    }

    @Override
    public NbtMap convertBlockEntity(NbtMap tag) {
        return this.blocks.convertBlockEntity(this.items.convert(tag));
    }
}
