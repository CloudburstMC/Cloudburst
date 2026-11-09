package org.cloudburstmc.server.level.provider.migration;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Normalizes and validates imported entity records without creating live entities.
 */
@UtilityClass
public class ImportedEntityData {

    /**
     * Converts entity records, normalizes numeric vectors to floats and resolves entity names.
     * Chunk ownership is assigned during destination finalization, not from the source key.
     */
    public static List<NbtMap> entities(List<NbtMap> tags, UnaryOperator<NbtMap> converter) {
        return tags.stream().map(converter).map(ImportedEntityData::normalizeVectors).map(tag -> {
            List<Float> position = tag.getList("Pos", NbtType.FLOAT);
            List<Float> rotation = tag.getList("Rotation", NbtType.FLOAT);
            if (position.size() != 3 || rotation.size() != 2
                    || position.stream().anyMatch(value -> !Float.isFinite(value))
                    || rotation.stream().anyMatch(value -> !Float.isFinite(value))) {
                throw new IllegalArgumentException("Invalid imported entity position or rotation: " + tag);
            }

            String id = tag.getString("identifier", tag.getString("id"));
            Identifier identifier = id.contains(":") ? Identifier.parse(id) : CloudEntityRegistry.get().getIdentifier(id);
            if (identifier == null) {
                throw new IllegalArgumentException("Unmapped imported entity: " + id);
            }

            return tag.toBuilder().putString("identifier", identifier.toString()).build();
        }).toList();
    }

    /**
     * Returns the containing chunk for a normalized entity position.
     * The result's X and Y components represent chunk X and Z respectively.
     */
    public static Vector2i chunkPosition(NbtMap tag) {
        List<Float> position = tag.getList("Pos", NbtType.FLOAT);
        if (position.size() != 3) {
            throw new IllegalArgumentException("Invalid imported entity position");
        }

        double x = position.get(0);
        double z = position.get(2);
        if (!Float.isFinite(position.get(1)) || !Double.isFinite(x) || !Double.isFinite(z)
                || x < Integer.MIN_VALUE || x >= 2147483648d || z < Integer.MIN_VALUE || z >= 2147483648d) {
            throw new IllegalArgumentException("Imported entity position is outside the supported coordinate range");
        }

        return Vector2i.from((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }

    /**
     * Converts block entities and requires their positions to belong to the destination chunk
     * and its build height range.
     */
    public static List<NbtMap> blockEntities(CloudChunkBuilder builder, List<NbtMap> tags, UnaryOperator<NbtMap> converter) {
        return tags.stream().map(converter).peek(tag -> {
            if (tag.getString("id").isBlank() || !tag.containsKey("x", NbtType.INT)
                    || !tag.containsKey("y", NbtType.INT) || !tag.containsKey("z", NbtType.INT)) {
                throw new IllegalArgumentException("Imported block entity has no ID or valid position: " + tag);
            }

            validateBlockEntityPosition(builder, tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        }).toList();
    }

    private static NbtMap normalizeVectors(NbtMap tag) {
        NbtMapBuilder normalized = tag.toBuilder();
        for (String name : List.of("Pos", "Motion", "Rotation")) {
            if (!tag.containsKey(name)) {
                continue;
            }

            int size = name.equals("Rotation") ? 2 : 3;
            if (!(tag.get(name) instanceof List<?> values) || values.size() != size) {
                throw new IllegalArgumentException("Invalid imported entity vector: " + name);
            }

            List<Float> coordinates = new ArrayList<>(size);
            for (Object value : values) {
                if (!(value instanceof Number number) || !Float.isFinite(number.floatValue())) {
                    throw new IllegalArgumentException("Invalid imported entity coordinate: " + name);
                }

                coordinates.add(number.floatValue());
            }

            normalized.putList(name, NbtType.FLOAT, coordinates);
        }

        return normalized.build();
    }

    private static void validateBlockEntityPosition(CloudChunkBuilder builder, int x, int y, int z) {
        if ((x >> 4) != builder.getX() || (z >> 4) != builder.getZ() || y < builder.getLevel().getMinHeight() || y >= builder.getLevel().getMaxHeight()) {
            throw new IllegalArgumentException("Imported block entity is outside its destination chunk: " + x + "," + y + "," + z);
        }
    }
}
