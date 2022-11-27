package org.cloudburstmc.server.level.provider.leveldb.serializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.ChunkDataLoader;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Log4j2
public class BlockEntitySerializer {

    public static void loadBlockEntities(DB db, ChunkBuilder builder) {
        byte[] key = LevelDBKey.BLOCK_ENTITIES.getKey(builder.getX(), builder.getZ());

        byte[] value = db.get(key);
        if (value == null) {
            return;
        }

        List<NbtMap> blockEntityTags = new ArrayList<>();
        try (ByteArrayInputStream stream = new ByteArrayInputStream(value);
             NBTInputStream nbtInputStream = NbtUtils.createReaderLE(stream)) {
            while (stream.available() > 0) {
                blockEntityTags.add((NbtMap) nbtInputStream.readTag());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        builder.dataLoader(new BlockEntityLoader(blockEntityTags));
    }

    public static void saveBlockEntities(WriteBatch db, CloudChunk chunk) {
        byte[] key = LevelDBKey.BLOCK_ENTITIES.getKey(chunk.getX(), chunk.getZ());
        if (chunk.getBlockEntities().isEmpty()) {
            db.delete(key);
            return;
        }

        Set<BaseBlockEntity> entities = chunk.getBlockEntities();

        byte[] value;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
            for (BaseBlockEntity entity : entities) {
                nbtOutputStream.writeTag(entity.getServerTag());
            }
            value = stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        db.put(key, value);
    }

    @RequiredArgsConstructor
    private static class BlockEntityLoader implements ChunkDataLoader {
        private static final BlockEntityRegistry REGISTRY = BlockEntityRegistry.get();
        private final List<NbtMap> blockEntityTags;

        @Override
        public boolean load(CloudChunk chunk) {
            boolean dirty = false;
            for (NbtMap tag : blockEntityTags) {
                if (tag != null) {
                    if (!tag.containsKey("id")) {
                        dirty = true;
                        continue;
                    }
                    Vector3i position = Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                    if ((position.getX() >> 4) != chunk.getX() || ((position.getZ() >> 4) != chunk.getZ())) {
                        dirty = true;
                        continue;
                    }
                    try {
                        BlockEntityType<?> type = REGISTRY.getBlockEntityType(tag.getString("id"));

                        BaseBlockEntity blockEntity = (BaseBlockEntity) REGISTRY.newEntity(type, chunk, position);
                        if (blockEntity == null) {
                            dirty = true;
                            continue;
                        }
                        blockEntity.loadAdditionalData(tag);
                    } catch (RegistryException e) {
                        log.throwing(e);
                        log.info("unknown: {}", tag.getString("id"));
                        dirty = true;
                    }
                }
            }
            return dirty;
        }
    }
}
