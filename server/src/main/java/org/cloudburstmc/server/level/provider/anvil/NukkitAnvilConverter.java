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
import org.cloudburstmc.server.level.provider.migration.NukkitBlockStorageReader;
import org.cloudburstmc.server.level.provider.migration.NukkitChunkGeneration;
import org.cloudburstmc.server.level.provider.migration.ImportedEntityData;
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

    /**
     * Reads the Level compound from an uncompressed numeric Nukkit Anvil envelope.
     * Requires a byte V marker and rejects Java DataVersion markers, but does not validate the version value.
     * Advances the buffer's reader index without releasing the caller-owned buffer.
     */
    public static NbtMap readChunk(ByteBuf chunkBuf) throws IOException {
        try (ByteBufInputStream stream = new ByteBufInputStream(chunkBuf); NBTInputStream nbtInputStream = NbtUtils.createReader(stream)) {
            if (!(nbtInputStream.readTag() instanceof NbtMap root) || !(root.get("Level") instanceof NbtMap level)) {
                throw new IOException("Anvil chunk must contain a Level compound");
            }

            if (root.containsKey("DataVersion") || level.containsKey("DataVersion") || !level.containsKey("V", NbtType.BYTE)) {
                throw new IOException("Unsupported Anvil chunk encoding. Numeric Bedrock Anvil chunks require a byte V field");
            }

            return level;
        }
    }

    /**
     * Converts one chunk using previously collected scheduled ticks.
     * The caller owns both the input buffer and the returned chunk.
     */
    public static ImportedChunk convertToCloudburst(CloudChunkBuilder chunkBuilder, ByteBuf chunkBuf,
                                                   NukkitBlockStateConverter blocks, NukkitItemDataConverter items,
                                                   NukkitAnvilPendingTicks pendingTicks) throws IOException {
        NbtMap tag = readChunk(chunkBuf);
        int version = tag.getByte("V") & 0xff;
        if (version > 4) {
            throw new IOException("Unknown Nukkit Anvil chunk version: " + version);
        }

        NukkitBlockStorageReader storage = new NukkitBlockStorageReader(blocks);
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

            BlockStorage[] layers = storage.readAnvilSection(sectionTag, version);
            sections[sectionIndex] = new CloudChunkSection(
                    chunkBuilder.getLevel().getServer().getBlockRegistry(),
                    layers, readLight(sectionTag, "BlockLight"), readLight(sectionTag, "SkyLight")
            );
        }

        if (tag.containsKey("ExtraData") && !tag.containsKey("ExtraData", NbtType.BYTE_ARRAY)) {
            throw new IOException("Nukkit Anvil ExtraData must be a byte array");
        }

        storage.readAnvilExtraData(tag.getByteArray("ExtraData"), sections,
                chunkBuilder.getLevel().getMinSectionY(), chunkBuilder.getLevel().getServer().getBlockRegistry());

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

        List<NbtMap> entities = ImportedEntityData.entities(tag.getList("Entities", NbtType.COMPOUND),
                entity -> blocks.convertEntity(items.convert(entity)));
        List<NbtMap> blockEntities = ImportedEntityData.blockEntities(chunkBuilder, tag.getList("TileEntities", NbtType.COMPOUND),
                blockEntity -> blocks.convertBlockEntity(items.convert(blockEntity)));
        NbtMap ticks = pendingTicks.take(chunkBuilder.getX(), chunkBuilder.getZ(), chunkBuilder.getLevel().getMinHeight(), chunkBuilder.getLevel().getMaxHeight(), tag);

        return new ImportedChunk(chunkBuilder.build(), entities, blockEntities, ticks);
    }

    private static byte[] readLight(NbtMap section, String name) throws IOException {
        byte[] bytes = section.getByteArray(name);
        if (bytes.length == 0 && !section.containsKey(name)) {
            return new byte[2048];
        }

        if (bytes.length != 2048) {
            throw new IOException("Invalid Nukkit Anvil " + name + " array in section " + section.getByte("Y"));
        }

        NibbleArray source = new NibbleArray(bytes);
        NibbleArray target = new NibbleArray(4096);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    target.set(CloudChunkSection.blockIndex(x, y, z), source.get(y << 8 | z << 4 | x));
                }
            }
        }

        return target.getData();
    }
}
