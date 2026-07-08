package org.cloudburstmc.server.level.provider.leveldb.serializer;

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
        if (pos == null || pos.size() < 3) {
            log.warn("Skipping entity with invalid position in chunk {},{}", chunk.getX(), chunk.getZ());
            return null;
        }
        Vector3f position = Vector3f.from(pos.get(0), pos.get(1), pos.get(2));

        List<Float> rotation = tag.getList("Rotation", NbtType.FLOAT);
        if (rotation == null || rotation.size() < 2) {
            log.warn("Skipping entity with invalid rotation in chunk {},{} at {}", chunk.getX(), chunk.getZ(), position);
            return null;
        }

        float yaw = rotation.get(0);
        float pitch = rotation.get(1);

        int entityChunkX = position.getFloorX() >> 4;
        int entityChunkZ = position.getFloorZ() >> 4;
        if (entityChunkX != chunk.getX() || entityChunkZ != chunk.getZ()) {
            String identifier = tag.containsKey("identifier") ? tag.getString("identifier") : "unknown";
            log.warn("Skipping entity {} stored in chunk {},{} but positioned in chunk {},{} at {}",
                    identifier, chunk.getX(), chunk.getZ(), entityChunkX, entityChunkZ, position);
            return null;
        }

        return Location.from(position, yaw, pitch, chunk.getLevel());
    }

    private record DataLoader(List<NbtMap> entityTags) implements ChunkDataLoader {
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
                    if (location == null) {
                        dirty = true;
                        continue;
                    }

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
                    log.warn("Skipping invalid entity data in chunk {},{}: {}", chunk.getX(), chunk.getZ(), e.toString());
                    log.debug("Invalid entity data", e);
                    dirty = true;
                }
            }
            return dirty;
        }
    }
}
