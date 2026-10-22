package org.cloudburstmc.server.level.provider.anvil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.cloudburstmc.server.level.provider.migration.NukkitBlockStateConverter;
import org.cloudburstmc.server.level.provider.migration.NukkitChunkGeneration;
import org.cloudburstmc.server.level.provider.migration.NukkitEntityData;
import org.cloudburstmc.server.level.provider.migration.NukkitItemDataConverter;
import org.cloudburstmc.server.utils.NibbleArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Converts persisted Nukkit Anvil terrain and entity records.
 */
@UtilityClass
public class NukkitAnvilConverter {

    public static ImportedChunk convertToCloudburst(CloudChunkBuilder chunkBuilder, ByteBuf chunkBuf, NukkitBlockStateConverter blocks, NukkitItemDataConverter items) throws IOException {
        NbtMap tag;
        try (ByteBufInputStream stream = new ByteBufInputStream(chunkBuf); NBTInputStream nbtInputStream = NbtUtils.createReader(stream)) {
            if (!(nbtInputStream.readTag() instanceof NbtMap root) || !(root.get("Level") instanceof NbtMap level)) {
                throw new IOException("Anvil chunk must contain a Level compound");
            }
            tag = level;
        }

        validateUnsupportedRecords(tag);

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

            sections[sectionIndex] = new CloudChunkSection(
                    chunkBuilder.getLevel().getServer().getBlockRegistry(),
                    new BlockStorage[]{blockStorage, new BlockStorage()}, blockLight, skyLight
            );
        }

        byte[] biomes = new byte[256];
        if (tag.containsKey("BiomeColors")) {
            int[] biomeColors = tag.getIntArray("BiomeColors");
            if (biomeColors.length != 256) {
                throw new IOException("Invalid Nukkit biome color array");
            }

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    biomes[x * 16 + z] = (byte) (biomeColors[z * 16 + x] >>> 24);
                }
            }
        } else {
            biomes = tag.getByteArray("Biomes");
        }

        if (biomes.length != 0 && biomes.length != 256) {
            throw new IOException("Invalid Nukkit biome array");
        }

        if (biomes.length == 256) {
            for (int i = 0; i < sections.length; i++) {
                CloudChunkSection section = sections[i];
                if (section == null) {
                    section = new CloudChunkSection(chunkBuilder.getLevel().getServer().getBlockRegistry());
                    sections[i] = section;
                }

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int biomeId = biomes[x * 16 + z] & 0xFF;
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

        chunkBuilder.setSections(sections);
        chunkBuilder.setHeightMap(heightMap);
        chunkBuilder.setGenerationStatus(NukkitChunkGeneration.readAnvil(tag));

        List<NbtMap> entities = NukkitEntityData.entities(chunkBuilder, tag.getList("Entities", NbtType.COMPOUND), blocks, items);
        List<NbtMap> blockEntities = NukkitEntityData.blockEntities(chunkBuilder, tag.getList("TileEntities", NbtType.COMPOUND), blocks, items);

        return new ImportedChunk(chunkBuilder.build(), entities, blockEntities, NbtMap.EMPTY);
    }

    private static void validateUnsupportedRecords(NbtMap tag) throws IOException {
        if (tag.containsKey("TileTicks") && (!(tag.get("TileTicks") instanceof List<?> ticks) || !ticks.isEmpty())) {
            throw new IOException("Nukkit Anvil TileTicks require a save-ID mapping and cannot be discarded");
        }

        byte[] extraData = tag.getByteArray("ExtraData");
        if (extraData.length != 0 && !Arrays.equals(extraData, new byte[Integer.BYTES])) {
            throw new IOException("Nukkit Anvil ExtraData conversion is not supported");
        }
    }

    private static int getAnvilIndex(int x, int y, int z) {
        return (y << 8) + (z << 4) + x;
    }
}
