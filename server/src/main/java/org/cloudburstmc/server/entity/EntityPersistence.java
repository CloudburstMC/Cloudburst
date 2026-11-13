package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.persistence.PersistentDataContainer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.data.CloudEntityData;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.persistence.CloudPersistentDataContainer;

import java.util.Map;
import java.util.Set;

public class EntityPersistence {

    private final CloudEntityData state;
    private final CloudPersistentDataContainer pluginData;
    private NbtMap loadedTag = NbtMap.EMPTY;

    public EntityPersistence(CloudEntityData state, Runnable markDirty) {
        this.state = state;
        this.pluginData = new CloudPersistentDataContainer(markDirty);
    }

    public CloudEntityData state() {
        return this.state;
    }

    public PersistentDataContainer pluginData() {
        return this.pluginData;
    }

    public void load(NbtMap tag) {
        this.pluginData.load(tag);
        ServerNbt.MAPPER.readInto(tag, this.state);
        this.loadedTag = tag;
    }

    public void writeState(NbtMapBuilder tag, Set<String> customKeys) {
        if (!this.loadedTag.isEmpty()) {
            Set<String> ownedKeys = ServerNbt.MAPPER.ownedKeys(this.state.getClass());
            for (Map.Entry<String, Object> entry : this.loadedTag.entrySet()) {
                String key = entry.getKey();
                if (!key.equals("identifier")
                        && !key.equals(CloudPersistentDataContainer.STORAGE_KEY)
                        && !ownedKeys.contains(key)
                        && !customKeys.contains(key)) {
                    tag.put(key, entry.getValue());
                }
            }
        }

        tag.putAll(ServerNbt.MAPPER.write(this.state));
    }

    public void writePluginData(NbtMapBuilder tag) {
        this.pluginData.save(tag);
    }
}
