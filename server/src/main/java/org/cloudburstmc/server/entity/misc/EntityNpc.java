package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.Npc;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;

public class EntityNpc extends CloudEntity implements Npc {

    public EntityNpc(EntityType<?> type, Location location) {
        super(type, location);
    }
}
