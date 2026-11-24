package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.CamelHusk;
import org.cloudburstmc.api.level.Location;

public class EntityCamelHusk extends EntityCamel implements CamelHusk {

    public EntityCamelHusk(EntityType<CamelHusk> type, Location location) {
        super(type, location);
    }

    @Override
    protected String getDefaultName() {
        return "Camel Husk";
    }
}
