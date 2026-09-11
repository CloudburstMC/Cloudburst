package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

/**
 * Serializes entity type metadata stored by item tags such as spawn eggs.
 */
public class EntityTypeSerializer implements ItemDataSerializer<EntityType<?>> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, EntityType<?> value) {
        tag.putString("ItemIdentifier", value.getId().toString());
    }

    @Override
    public EntityType<?> deserialize(Identifier id, NbtMap tag) {
        if (!tag.containsKey("ItemIdentifier")) {
            return null;
        }

        return CloudEntityRegistry.get().get(Identifier.parse(tag.getString("ItemIdentifier"))).orElse(null);
    }
}
