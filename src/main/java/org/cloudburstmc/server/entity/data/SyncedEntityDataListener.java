package org.cloudburstmc.server.entity.data;

import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;

public interface SyncedEntityDataListener {
    void onDataChange(EntityDataMap changeSet);
}
