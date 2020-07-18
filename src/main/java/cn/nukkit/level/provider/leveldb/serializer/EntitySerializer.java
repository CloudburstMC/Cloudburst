package cn.nukkit.level.provider.leveldb.serializer;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityType;
import cn.nukkit.level.Location;
import cn.nukkit.level.chunk.Chunk;
import cn.nukkit.level.chunk.ChunkBuilder;
import cn.nukkit.level.chunk.ChunkDataLoader;
import cn.nukkit.level.provider.leveldb.LevelDBKey;
import cn.nukkit.registry.EntityRegistry;
import cn.nukkit.registry.RegistryException;
import cn.nukkit.utils.Identifier;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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

    public static void saveEntities(WriteBatch db, Chunk chunk) {
        byte[] key = LevelDBKey.ENTITIES.getKey(chunk.getX(), chunk.getZ());
        Set<Entity> entities = chunk.getEntities();
        if (entities.isEmpty()) {
            db.delete(key);
            return;
        }

        byte[] value;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             NBTOutputStream nbtOutputStream = NbtUtils.createWriterLE(stream)) {
            for (Entity entity : entities) {
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
    private static Location getLocation(NbtMap tag, Chunk chunk) {
        List<NbtMap> pos = (List) tag.getList("Pos", NbtType.FLOAT);
        Vector3f position = Vector3f.from((float) pos.get(0).get(0)
                , (float) pos.get(1).get(0)
                , (float) pos.get(2).get(0));

        List<NbtMap> rotation = (List) tag.getList("Rotation", NbtType.FLOAT);
        float yaw = (float) rotation.get(0).get(0);
        float pitch = (float) rotation.get(1).get(0);

        checkArgument(position.getFloorX() >> 4 == chunk.getX() && position.getFloorZ() >> 4 == chunk.getZ(),
                "Entity is not in chunk of origin");

        return Location.from(position, yaw, pitch, chunk.getLevel());
    }

    @RequiredArgsConstructor
    private static class DataLoader implements ChunkDataLoader {
        private final List<NbtMap> entityTags;

        @Override
        public boolean load(Chunk chunk) {
            boolean dirty = false;
            for (NbtMap entityTag : entityTags) {
                if (!entityTag.containsKey("identifier")) {
                    dirty = true;
                    continue;
                }
                Location location = getLocation(entityTag, chunk);
                Identifier identifier = Identifier.fromString(entityTag.getString("identifier"));
                EntityRegistry registry = EntityRegistry.get();
                EntityType<?> type = registry.getEntityType(identifier);
                try {
                    Entity entity = registry.newEntity(type, location);
                    if (entity != null) {
                        entity.loadAdditionalData(entityTag);
                    }
                } catch (RegistryException e) {
                    dirty = true;
                }
            }
            return dirty;
        }
    }
}
