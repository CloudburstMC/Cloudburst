package org.cloudburstmc.server.entity.data;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

import java.util.EnumMap;

public class SyncedEntityData {
    private final EntityDataMap data = new EntityDataMap();
    private final EntityDataMap dataChangeSet = new EntityDataMap();
    private final EnumMap<EntityFlag, Boolean> flags = new EnumMap<>(EntityFlag.class);
    private final SyncedEntityDataListener listener;

    public SyncedEntityData(SyncedEntityDataListener listener) {
        this.listener = listener;
    }

    public void update() {
        if (this.dataChangeSet.isEmpty()) {
            return;
        }

        this.listener.onDataChange(this.dataChangeSet);
        this.dataChangeSet.clear();
    }

    public void putAllIn(EntityDataMap map) {
        map.putAll(this.data);
    }

    public void putFlagsIn(EntityDataMap map) {
        map.putFlags(this.flags);
    }

    public boolean contains(EntityDataType<?> data) {
        return this.data.containsKey(data);
    }

    public <T> T get(EntityDataType<T> type) {
        return this.data.get(type);
    }

    public <T> void set(EntityDataType<T> type, T value) {
        this.data.put(type, value);
        this.dataChangeSet.put(type, value);
    }

    public boolean getFlag(EntityFlag flag) {
        return flags.getOrDefault(flag, false);
    }

    public void setFlag(EntityFlag flag, boolean value) {
        Boolean oldValue = this.flags.get(flag);
        if (oldValue == null || value != oldValue) {
            this.flags.put(flag, value);
            this.dataChangeSet.putFlags(this.flags);
        }
    }
}
