package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.entity.EntitySnapshot;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.entity.CloudEntitySnapshot;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

public class EntitySnapshotSerializer implements ItemDataComponentSerializer<EntitySnapshot> {
    private static final String TAG = "cloudburst:entity_data";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, EntitySnapshot value) {
        if (!(value instanceof CloudEntitySnapshot snapshot)) {
            throw new IllegalArgumentException("Entity data must originate from an entity snapshot");
        }

        tag.putCompound(TAG, NbtMap.builder()
                .putString("type", snapshot.getType().getId().toString())
                .putCompound("data", snapshot.data())
                .build());
    }

    @Override
    public EntitySnapshot deserialize(Identifier id, NbtMap tag) {
        if (!tag.containsKey(TAG)) {
            return null;
        }

        if (!tag.containsKey(TAG, NbtType.COMPOUND)) {
            throw new RegistryException("Invalid item entity data on " + id);
        }

        NbtMap snapshot = tag.getCompound(TAG);
        if (!snapshot.containsKey("type", NbtType.STRING) || !snapshot.containsKey("data", NbtType.COMPOUND)) {
            throw new RegistryException("Invalid item entity snapshot on " + id);
        }

        Identifier typeId = Identifier.parse(snapshot.getString("type"));
        EntityType<?> type = CloudEntityRegistry.get().get(typeId)
                .orElseThrow(() -> new RegistryException("Unknown snapshot entity type " + typeId));
        return new CloudEntitySnapshot(type, snapshot.getCompound("data"));
    }
}
