package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.registry.EntityRegistry;

@SuppressWarnings("rawtypes")
public class EntityTypeSerializer implements ItemDataSerializer<EntityType> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, EntityType value) {
        dataTag.putString("ItemIdentifier", value.getIdentifier().toString());
    }

    @Override
    public EntityType deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        return EntityRegistry.get().getEntityType(Identifier.fromString(dataTag.getString("ItemIdentifier", "unknown")));
    }
}
