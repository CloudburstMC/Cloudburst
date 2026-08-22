package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.registry.EntityRegistry;

/**
 * Serializes entity type metadata stored by item tags such as spawn eggs.
 */
public class EntityTypeSerializer implements ItemDataSerializer<EntityType<?>> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, EntityType<?> value) {
        tag.putString("ItemIdentifier", value.getIdentifier().toString());
    }

    @Override
    public EntityType<?> deserialize(Identifier id, NbtMap tag) {
        if (!tag.containsKey("ItemIdentifier")) {
            return null;
        }

        return EntityRegistry.get().getEntityType(Identifier.parse(tag.getString("ItemIdentifier")));
    }
}
