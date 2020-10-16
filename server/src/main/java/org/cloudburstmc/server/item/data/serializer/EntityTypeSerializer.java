package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

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
