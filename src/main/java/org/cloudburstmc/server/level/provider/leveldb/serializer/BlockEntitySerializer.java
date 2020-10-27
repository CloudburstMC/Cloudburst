package org.cloudburstmc.server.level.provider.leveldb.serializer;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.ChunkDataLoader;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.RegistryException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public static void saveBlockEntities(WriteBatch db, Chunk chunk) {
        byte[] key = LevelDBKey.BLOCK_ENTITIES.getKey(chunk.getX(), chunk.getZ());
        if (chunk.getBlockEntities().isEmpty()) {
            db.delete(key);
            return;
        }

        Collection<BlockEntity> entities = chunk.getBlockEntities();

        byte[] value;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
            for (BlockEntity entity : entities) {
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
        public boolean load(Chunk chunk) {
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

                        BlockEntity blockEntity = REGISTRY.newEntity(type, chunk, position);
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
