package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.EntityRegistry;

@SuppressWarnings("rawtypes")
public class EntityTypeSerializer implements ItemDataSerializer<EntityType> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, EntityType value) {
        tag.putString("ItemIdentifier", value.getIdentifier().toString());
    }

    @Override
    public EntityType deserialize(Identifier id, NbtMap tag) {
        return EntityRegistry.get().getEntityType(Identifier.fromString(tag.getString("ItemIdentifier", "unknown")));
    }
}
