package org.cloudburstmc.server.level.provider.leveldb.serializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.ChunkDataLoader;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@Log4j2
public class EntitySerializer {

    public static void loadEntities(DB db, ChunkBuilder builder) {
        byte[] key = LevelDBKey.ENTITIES.getKey(builder.getX(), builder.getZ());

        byte[] value = db.get(key);
        if (value == null) {
            return;
        }

        List<NbtMap> entityTags = new ArrayList<>();
        try (ByteArrayInputStream stream = new ByteArrayInputStream(value);
             NBTInputStream nbtInputStream = NbtUtils.createReaderLE(stream)) {
            while (stream.available() > 0) {
                entityTags.add((NbtMap) nbtInputStream.readTag());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        builder.dataLoader(new DataLoader(entityTags));
    }

    public static void saveEntities(WriteBatch db, CloudChunk chunk) {
        byte[] key = LevelDBKey.ENTITIES.getKey(chunk.getX(), chunk.getZ());
        Set<CloudEntity> entities = chunk.getEntities();
        if (entities.isEmpty()) {
            db.delete(key);
            return;
        }

        byte[] value;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
            for (CloudEntity entity : entities) {
                NbtMapBuilder tag = NbtMap.builder();
                entity.saveAdditionalData(tag);
                nbtOutputStream.writeTag(tag.build());
            }
            value = stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        db.put(key, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Location getLocation(NbtMap tag, CloudChunk chunk) {
        List<Float> pos = tag.getList("Pos", NbtType.FLOAT);
        Vector3f position = Vector3f.from(pos.get(0), pos.get(1), pos.get(2));

        List<Float> rotation = (List) tag.getList("Rotation", NbtType.FLOAT);
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
        public boolean load(CloudChunk chunk) {
            boolean dirty = false;
            for (NbtMap entityTag : entityTags) {
                try {
                    if (!entityTag.containsKey("identifier")) {
                        dirty = true;
                        continue;
                    }
                    Location location = getLocation(entityTag, chunk);
                    Identifier identifier = Identifier.parse(entityTag.getString("identifier"));
                    EntityRegistry registry = EntityRegistry.get();
                    EntityType<?> type = registry.getEntityType(identifier);
                    if (type == null) {
                        log.warn("Unknown entity type {}", identifier);
                        dirty = true;
                        continue;
                    }
                    try {
                        CloudEntity entity = (CloudEntity) registry.newEntity(type, location);
                        if (entity != null) {
                            entity.loadAdditionalData(entityTag);
                        }
                    } catch (RegistryException e) {
                        dirty = true;
                    }
                } catch (Exception e) {
                    log.throwing(e);
                }
            }
            return dirty;
        }
    }
}
