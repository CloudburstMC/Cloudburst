package org.cloudburstmc.server.level.provider.anvil;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.nbt.NbtUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.chunk.*;
import org.cloudburstmc.server.level.provider.LegacyBlockConverter;
import org.cloudburstmc.server.level.provider.anvil.palette.BiomePalette;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.NibbleArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

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

        ChunkSection[] sections = new ChunkSection[Chunk.SECTION_COUNT];

        // Reusable array for performance
        final int[] blockState = new int[2];
        BlockRegistry blockRegistry = BlockRegistry.get();
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
                        int cloudburstIndex = ChunkSection.blockIndex(blockX, blockY, blockZ);
                        blockState[0] = blocks[anvilIndex] & 0xff;
                        blockState[1] = data.get(anvilIndex);
                        legacyBlockConverter.convertBlockState(blockState);
                        blockStorage.setBlock(cloudburstIndex, blockRegistry.getBlock(blockState[0], blockState[1]));
                    }
                }
            }

            sections[y] = new ChunkSection(new BlockStorage[]{blockStorage, new BlockStorage()}, blockLight, skyLight);
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
        chunkBuilder.biomes(biomes);

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
            chunkBuilder.state(IChunk.STATE_GENERATED);
        }
        if (tag.getBoolean("TerrainPopulated")) {
            chunkBuilder.state(IChunk.STATE_POPULATED);
        }
    }

    public static NbtMap convertToAnvil(Chunk chunk) {
        throw new UnsupportedOperationException();
    }

    private static int getAnvilIndex(int x, int y, int z) {
        return (y << 8) + (z << 4) + x;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Location getLocation(NbtMap tag, Chunk chunk) {
        List<Float> pos = tag.getList("Pos", NbtType.FLOAT);
        if (pos == null || pos.size() < 3) return null;

        Vector3f position = Vector3f.from(pos.get(0), pos.get(1), pos.get(2));

        List<Float> rotation = tag.getList("Rotation", NbtType.FLOAT);
        float yaw = rotation.get(0);
        float pitch = rotation.get(1);

        checkArgument(position.getFloorX() >> 4 == chunk.getX() && position.getFloorZ() >> 4 == chunk.getZ(),
                "Entity is not in chunk of origin");

        return Location.from(position, yaw, pitch, chunk.getLevel());
    }

    @RequiredArgsConstructor
    private static class DataLoader implements ChunkDataLoader {
        private final List<NbtMap> entityTags;

        @Override
        public boolean load(Chunk chunk) {
            EntityRegistry registry = EntityRegistry.get();
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
                Vector3f position = location.getPosition();
                if ((position.getFloorX() >> 4) != chunk.getX() || ((position.getFloorZ() >> 4) != chunk.getZ())) {
                    dirty = true;
                    continue;
                }
                Identifier identifier = registry.getIdentifier(entityTag.getString("id"));
                if (identifier == null) {
                    dirty = true;
                    continue;
                }
                EntityType<?> type = registry.getEntityType(identifier);
                Entity entity = registry.newEntity(type, location);
                entity.loadAdditionalData(entityTag);
            }
            return dirty;
        }
    }

    @RequiredArgsConstructor
    private static class TileLoader implements ChunkDataLoader {
        private static final BlockEntityRegistry REGISTRY = BlockEntityRegistry.get();
        private final List<NbtMap> tileTags;

        @Override
        public boolean load(Chunk chunk) {
            boolean dirty = false;
            for (NbtMap tag : tileTags) {
                if (tag != null) {
                    if (!tag.containsKey("id")) {
                        dirty = true;
                        continue;
                    }
                    Vector3i position = Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("y"));
                    if ((position.getX() >> 4) != chunk.getX() || ((position.getZ() >> 4) != chunk.getZ())) {
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
