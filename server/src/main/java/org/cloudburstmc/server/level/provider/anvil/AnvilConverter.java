package org.cloudburstmc.server.level.provider.anvil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.cloudburstmc.server.level.provider.migration.NukkitBlockStateConverter;
import org.cloudburstmc.server.level.provider.migration.NukkitEntityData;
import org.cloudburstmc.server.level.provider.migration.NukkitItemDataConverter;
import org.cloudburstmc.server.utils.NibbleArray;

import java.io.IOException;
import java.util.Arrays;

public class AnvilConverter {

    public static ImportedChunk convertToCloudburst(CloudChunkBuilder chunkBuilder, ByteBuf chunkBuf, NukkitBlockStateConverter blocks, NukkitItemDataConverter items) throws IOException {
        NbtMap tag;
        try (ByteBufInputStream stream = new ByteBufInputStream(chunkBuf); NBTInputStream nbtInputStream = NbtUtils.createReader(stream)) {
            tag = (NbtMap) nbtInputStream.readTag();
            if (!tag.containsKey("Level") || !(tag.get("Level") instanceof NbtMap)) {
                throw new IllegalArgumentException("No level tag found in chunk data");
            }
            tag = tag.getCompound("Level");
        }

        CloudChunkSection[] sections = new CloudChunkSection[chunkBuilder.getLevel().getSectionsCount()];
        for (NbtMap sectionTag : tag.getList("Sections", NbtType.COMPOUND)) {
            int y = sectionTag.getByte("Y");
            int sectionIndex = y - chunkBuilder.getLevel().getMinSectionY();
            if (sectionIndex < 0 || sectionIndex >= sections.length) {
                throw new IOException("Imported section is outside the destination height: " + y);
            }

            if (sections[sectionIndex] != null) {
                throw new IOException("Duplicate imported section: " + y);
            }

            byte[] blockIds = sectionTag.getByteArray("Blocks");
            byte[] metadata = sectionTag.getByteArray("Data");
            if (blockIds.length != 4096 || metadata.length != 2048) {
                throw new IOException("Invalid Nukkit block arrays in section " + y);
            }

            NibbleArray data = new NibbleArray(metadata);
            byte[] blockLight = sectionTag.getByteArray("BlockLight");
            byte[] skyLight = sectionTag.getByteArray("SkyLight");

            BlockStorage blockStorage = new BlockStorage();
            for (int blockX = 0; blockX < 16; blockX++) {
                for (int blockZ = 0; blockZ < 16; blockZ++) {
                    for (int blockY = 0; blockY < 16; blockY++) {
                        int anvilIndex = getAnvilIndex(blockX, blockY, blockZ);
                        int cloudburstIndex = CloudChunkSection.blockIndex(blockX, blockY, blockZ);
                        blockStorage.setBlock(cloudburstIndex, blocks.read(blockIds[anvilIndex] & 0xff, data.get(anvilIndex)));
                    }
                }
            }

            sections[sectionIndex] = new CloudChunkSection(chunkBuilder.getLevel().getServer().getBlockRegistry(), new BlockStorage[]{blockStorage, new BlockStorage()}, blockLight, skyLight);
        }
        chunkBuilder.setSections(sections);

        byte[] biomes;
        if (tag.containsKey("BiomeColors")) {
            int[] biomeColors = tag.getIntArray("BiomeColors");
            biomes = new byte[256];
            if (biomeColors.length != 256) {
                throw new IOException("Invalid Nukkit biome color array");
            }

            for (int i = 0; i < 256; i++) {
                biomes[i] = (byte) (biomeColors[i] >>> 24);
            }
        } else {
            biomes = tag.getByteArray("Biomes");
        }

        if (biomes.length != 0 && biomes.length != 256) {
            throw new IOException("Invalid Nukkit biome array");
        }

        if (biomes.length == 256) {
            for (CloudChunkSection section : sections) {
                if (section == null) {
                    continue;
                }

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int biomeId = biomes[z * 16 + x] & 0xFF;
                        section.fillColumnBiome(x, z, biomeId);
                    }
                }
            }
        }

        int[] anvilHeightMap = tag.getIntArray("HeightMap");
        int[] heightMap;
        if (anvilHeightMap.length == 0) {
            heightMap = new int[256];
            Arrays.fill(heightMap, chunkBuilder.getLevel().getMinHeight());
        } else if (anvilHeightMap.length == 256) {
            heightMap = anvilHeightMap.clone();
        } else {
            throw new IOException("Invalid Nukkit height map");
        }

        chunkBuilder.setHeightMap(heightMap);

        if (tag.getBoolean("TerrainGenerated")) {
            chunkBuilder.setGenerationStatus(ChunkGenerationStatus.GENERATED);
        }

        if (tag.getBoolean("TerrainPopulated")) {
            chunkBuilder.setGenerationStatus(ChunkGenerationStatus.POPULATED);
        }

        return new ImportedChunk(chunkBuilder.build(),
                NukkitEntityData.entities(chunkBuilder, tag.getList("Entities", NbtType.COMPOUND), blocks, items),
                NukkitEntityData.blockEntities(chunkBuilder, tag.getList("TileEntities", NbtType.COMPOUND), blocks, items), NbtMap.EMPTY);
    }

    private static int getAnvilIndex(int x, int y, int z) {
        return (y << 8) + (z << 4) + x;
    }
}
