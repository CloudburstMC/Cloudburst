package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.registry.RegistryUtils;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts numeric Nukkit items, including nested inventories, to named item data.
 */
public class NukkitItemDataConverter {

    private final Map<Integer, String> names;
    private final Map<String, Map<String, String>> variants;

    public NukkitItemDataConverter() throws IOException {
        Map<String, Integer> ids;
        try (InputStream input = RegistryUtils.getOrAssertResource("migration/nukkit/item_ids.json")) {
            ids = Bootstrap.JSON_MAPPER.readValue(input, new TypeReference<>() {});
        }

        Map<Integer, String> names = new HashMap<>();
        ids.forEach((name, id) -> names.put(id, name));
        names.put(0, "minecraft:air");
        this.names = Map.copyOf(names);

        try (InputStream input = RegistryUtils.getOrAssertResource("migration/nukkit/item_variants.json")) {
            this.variants = Bootstrap.JSON_MAPPER.readValue(input, new TypeReference<>() {});
        }
    }

    public NbtMap convert(NbtMap source) {
        NbtMapBuilder converted = source.toBuilder();
        source.forEach((name, value) -> {
            if (value instanceof NbtMap compound) {
                converted.putCompound(name, convert(compound));
            } else if (value instanceof NbtList<?> list && list.getType() == NbtType.COMPOUND) {
                converted.putList(name, NbtType.COMPOUND, list.stream().map(entry -> convert((NbtMap) entry)).toList());
            }
        });

        if (!source.containsKey("Name") && source.containsKey("id", NbtType.SHORT) && source.containsKey("Count", NbtType.BYTE)) {
            int id = source.getShort("id");
            String name = this.names.get(id);
            if (name == null) {
                throw new IllegalArgumentException("Unmapped Nukkit item: " + id);
            }

            Map<String, String> variants = this.variants.get(name);
            if (variants != null) {
                int damage = source.getShort("Damage");
                String variant = variants.get(Integer.toString(damage));
                if (variant == null || variant.equals("protocol")) {
                    throw new IllegalArgumentException("Unmapped Nukkit item variant: " + id + ":" + damage);
                }

                name = variant;
                converted.putShort("Damage", (short) 0);
            }

            converted.putString("Name", name);
            converted.remove("id");
        }

        return converted.build();
    }
}
