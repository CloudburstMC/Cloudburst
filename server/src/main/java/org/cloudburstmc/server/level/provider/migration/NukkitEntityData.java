package org.cloudburstmc.server.level.provider.migration;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.List;

/**
 * Converts saved Nukkit entity records without instantiating live entities.
 */
@UtilityClass
public class NukkitEntityData {

    public static List<NbtMap> entities(CloudChunkBuilder builder, List<NbtMap> tags, NukkitBlockStateConverter blocks, NukkitItemDataConverter items) {
        return tags.stream().map(items::convert).map(blocks::convertEntity).map(tag -> {
            List<Float> position = tag.getList("Pos", NbtType.FLOAT);
            List<Float> rotation = tag.getList("Rotation", NbtType.FLOAT);
            if (position.size() != 3 || rotation.size() != 2
                    || position.stream().anyMatch(value -> !Float.isFinite(value))
                    || rotation.stream().anyMatch(value -> !Float.isFinite(value))) {
                throw new IllegalArgumentException("Invalid imported entity position or rotation: " + tag);
            }

            validatePosition(builder, (int) Math.floor(position.get(0)), position.get(1), (int) Math.floor(position.get(2)));
            String id = tag.getString("identifier", tag.getString("id"));
            Identifier identifier = id.contains(":") ? Identifier.parse(id) : CloudEntityRegistry.get().getIdentifier(id);
            if (identifier == null) {
                throw new IllegalArgumentException("Unmapped Nukkit entity: " + id);
            }

            return tag.toBuilder().putString("identifier", identifier.toString()).build();
        }).toList();
    }

    public static List<NbtMap> blockEntities(CloudChunkBuilder builder, List<NbtMap> tags, NukkitBlockStateConverter blocks, NukkitItemDataConverter items) {
        return tags.stream().map(items::convert).map(blocks::convertBlockEntity).peek(tag -> {
            if (tag.getString("id").isBlank()) {
                throw new IllegalArgumentException("Imported block entity has no ID: " + tag);
            }

            validatePosition(builder, tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        }).toList();
    }

    private static void validatePosition(CloudChunkBuilder builder, int x, float y, int z) {
        if ((x >> 4) != builder.getX() || (z >> 4) != builder.getZ() || y < builder.getLevel().getMinHeight() || y >= builder.getLevel().getMaxHeight()) {
            throw new IllegalArgumentException("Imported entity is outside its destination chunk");
        }
    }
}
