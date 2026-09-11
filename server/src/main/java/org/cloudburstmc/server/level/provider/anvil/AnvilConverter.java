package org.cloudburstmc.server.level.provider.anvil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.chunk.*;
import org.cloudburstmc.server.level.provider.LegacyBlockConverter;
import org.cloudburstmc.server.level.provider.anvil.palette.BiomePalette;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.utils.NibbleArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class AnvilConverter {

    public static void convertToCloudburst(ChunkBuilder chunkBuilder, ByteBuf chunkBuf) throws IOException {
        NbtMap tag;

        try (ByteBufInputStream stream = new ByteBufInputStream(chunkBuf);
             NBTInputStream nbtInputStream = NbtUtils.createReader(stream)) {
            tag = (NbtMap) nbtInputStream.readTag();

            if (!tag.containsKey("Level") || !(tag.get("Level") instanceof NbtMap)) {
                throw new IllegalArgumentException("No level tag found in chunk data");
            }
            tag = tag.getCompound("Level");
        }

        CloudChunkSection[] sections = new CloudChunkSection[chunkBuilder.getLevel().getSectionsCount()];

        // Reusable array for performance
        final int[] blockState = new int[2];
        CloudBlockRegistry blockRegistry = CloudBlockRegistry.REGISTRY;
        LegacyBlockConverter legacyBlockConverter = LegacyBlockConverter.get();

        // Chunk sections
        for (NbtMap sectionTag : tag.getList("Sections", NbtType.COMPOUND)) {
            int y = sectionTag.getByte("Y");
            if (y >= 16) {
                continue;
            }

            byte[] blocks = sectionTag.getByteArray("Blocks");
            NibbleArray data = new NibbleArray(sectionTag.getByteArray("Data"));
            byte[] blockLight = sectionTag.getByteArray("BlockLight");
            byte[] skyLight = sectionTag.getByteArray("SkyLight");

            BlockStorage blockStorage = new BlockStorage();
            // Convert YZX to XZY
            for (int blockX = 0; blockX < 16; blockX++) {
                for (int blockZ = 0; blockZ < 16; blockZ++) {
                    for (int blockY = 0; blockY < 16; blockY++) {
                        int anvilIndex = getAnvilIndex(blockX, blockY, blockZ);
                        int cloudburstIndex = CloudChunkSection.blockIndex(blockX, blockY, blockZ);
                        blockState[0] = blocks[anvilIndex] & 0xff;
                        blockState[1] = data.get(anvilIndex);
                        legacyBlockConverter.convertBlockState(blockState);
                        blockStorage.setBlock(cloudburstIndex, blockRegistry.getBlock(blockState[0], blockState[1]));
                    }
                }
            }

            sections[y] = new CloudChunkSection(chunkBuilder.getLevel().getServer().getBlockRegistry(),
                    new BlockStorage[]{blockStorage, new BlockStorage()}, blockLight, skyLight);
        }
        chunkBuilder.sections(sections);

        byte[] biomes;
        if (tag.containsKey("BiomeColors")) {
            int[] biomeColors = tag.getIntArray("BiomeColors");
            biomes = new byte[256];
            if (biomeColors != null && biomeColors.length == 256) {
                BiomePalette palette = new BiomePalette(biomeColors);
                for (int i = 0; i < 256; i++) {
                    biomes[i] = (byte) (palette.get(i) >> 24);
                }
            }
        } else {
            biomes = tag.getByteArray("Biomes");
        }
        // Convert legacy 2D biome array (one entry per XZ column) to 3D per-section biome storage
        if (biomes != null && biomes.length >= 256) {
            for (CloudChunkSection section : sections) {
                if (section == null) continue;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int biomeId = biomes[z * 16 + x] & 0xFF;
                        section.fillColumnBiome(x, z, biomeId);
                    }
                }
            }
        }

        int[] anvilHeightMap = tag.getIntArray("HeightMap");
        int[] heightMap = new int[256];
        if (anvilHeightMap.length != 256) {
            Arrays.fill(heightMap, (byte) 255);
        } else {
            for (int i = 0; i < heightMap.length; i++) {
                heightMap[i] = (byte) anvilHeightMap[i];
            }
        }
        chunkBuilder.heightMap(heightMap);


        chunkBuilder.dataLoader(new DataLoader(tag.getList("Entities", NbtType.COMPOUND)));
        chunkBuilder.dataLoader(new TileLoader(tag.getList("TileEntities", NbtType.COMPOUND)));

        List<NbtMap> updateEntries = tag.getList("TileTicks", NbtType.COMPOUND);

        if (updateEntries != null && updateEntries.size() > 0) {
            for (NbtMap entryTag : updateEntries) {
//                Block block; //TODO: converter

//                try {
//                    String name = entryTag.getString("i");
//
//
//                    @SuppressWarnings("unchecked")
//                    Class<? extends BlockState> clazz = (Class<? extends BlockState>) Class.forName("cn.nukkit.block." + name);
//
//                    Constructor<? extends BlockState> constructor = clazz.getDeclaredConstructor();
//                    constructor.setAccessible(true);
//                    block = constructor.newInstance();
//                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
//                        IllegalAccessException | InvocationTargetException e) {
//                    continue;
//                }

//                block.setPosition(Vector3i.from(
//                        entryTag.getInt("x"),
//                        entryTag.getInt("y"),
//                        entryTag.getInt("z")
//                ));
//
//                chunkBuilder.blockUpdate(BlockUpdate.of(block, block.getPosition(), entryTag.getInt("t"),
//                        entryTag.getInt("p"), false));
            }
        }

        if (tag.getBoolean("TerrainGenerated")) {
            chunkBuilder.state(Chunk.STATE_GENERATED);
        }
        if (tag.getBoolean("TerrainPopulated")) {
            chunkBuilder.state(Chunk.STATE_POPULATED);
        }
    }

    public static NbtMap convertToAnvil(CloudChunk chunk) {
        throw new UnsupportedOperationException();
    }

    private static int getAnvilIndex(int x, int y, int z) {
        return (y << 8) + (z << 4) + x;
    }

    private static Location getLocation(NbtMap tag, CloudChunk chunk) {
        List<Float> pos = tag.getList("Pos", NbtType.FLOAT);
        if (pos == null || pos.size() < 3) {
            log.warn("Skipping legacy entity with invalid position in chunk {},{}", chunk.getX(), chunk.getZ());
            return null;
        }

        Vector3f position = Vector3f.from(pos.get(0), pos.get(1), pos.get(2));
        List<Float> rotation = tag.getList("Rotation", NbtType.FLOAT);
        if (rotation == null || rotation.size() < 2) {
            log.warn("Skipping legacy entity with invalid rotation in chunk {},{} at {}", chunk.getX(), chunk.getZ(), position);
            return null;
        }

        float yaw = rotation.get(0);
        float pitch = rotation.get(1);

        int entityChunkX = position.getFloorX() >> 4;
        int entityChunkZ = position.getFloorZ() >> 4;
        if (entityChunkX != chunk.getX() || entityChunkZ != chunk.getZ()) {
            String identifier = tag.containsKey("id") ? tag.getString("id") : "unknown";
            log.warn("Skipping legacy entity {} stored in chunk {},{} but positioned in chunk {},{} at {}",
                    identifier, chunk.getX(), chunk.getZ(), entityChunkX, entityChunkZ, position);
            return null;
        }

        return Location.from(position, yaw, pitch, chunk.getLevel());
    }

    private record DataLoader(List<NbtMap> entityTags) implements ChunkDataLoader {
        @Override
        public boolean load(CloudChunk chunk) {
            CloudEntityRegistry registry = CloudEntityRegistry.get();
            boolean dirty = false;
            for (NbtMap entityTag : entityTags) {
                if (!entityTag.containsKey("id")) {
                    dirty = true;
                    continue;
                }
                Location location = getLocation(entityTag, chunk);
                if (location == null) {
                    dirty = true; // Entity doesn't have a location?!?
                    continue;
                }
                Identifier identifier = registry.getIdentifier(entityTag.getString("id"));
                if (identifier == null) {
                    dirty = true;
                    continue;
                }
                EntityType<?> type = registry.getOrCreateUnknownType(identifier);
                try {
                    CloudEntity entity = (CloudEntity) registry.newEntity(type, location);
                    entity.loadAdditionalData(entityTag);
                    entity.spawn();
                } catch (Exception e) {
                    log.warn("Skipping invalid legacy entity data in chunk {},{}: {}", chunk.getX(), chunk.getZ(), e.toString());
                    log.debug("Invalid legacy entity data", e);
                    dirty = true;
                }
            }
            return dirty;
        }
    }

    private record TileLoader(List<NbtMap> tileTags) implements ChunkDataLoader {
        private static final CloudBlockEntityRegistry REGISTRY = CloudBlockEntityRegistry.get();

        @Override
        public boolean load(CloudChunk chunk) {
            boolean dirty = false;
            for (NbtMap tag : tileTags) {
                if (tag != null) {
                    if (!tag.containsKey("id")) {
                        dirty = true;
                        continue;
                    }
                    Vector3i position = Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                    if ((position.getX() >> 4) != chunk.getX() || ((position.getZ() >> 4) != chunk.getZ())) {
                        log.warn("Skipping legacy block entity {} stored in chunk {},{} but positioned at {}",
                                tag.getString("id"), chunk.getX(), chunk.getZ(), position);
                        dirty = true;
                        continue;
                    }
                    BlockEntityType<?> type = REGISTRY.getBlockEntityType(tag.getString("id"));

                    BlockEntity blockEntity = REGISTRY.newEntity(type, chunk, position);
                    if (blockEntity == null) {
                        dirty = true;
                    }
                }
            }
            return dirty;
        }
    }
}
