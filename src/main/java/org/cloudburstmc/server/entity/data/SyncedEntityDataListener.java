package org.cloudburstmc.server.entity.data;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;

public interface SyncedEntityDataListener {
    void onDataChange(EntityDataMap changeSet);
}
