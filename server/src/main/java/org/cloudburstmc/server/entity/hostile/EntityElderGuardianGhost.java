package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.ElderGuardianGhost;
import org.cloudburstmc.api.level.Location;

public class EntityElderGuardianGhost extends EntityHostile implements ElderGuardianGhost {

    public EntityElderGuardianGhost(EntityType<ElderGuardianGhost> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 1.9975f;
    }

    @Override
    public float getHeight() {
        return 1.9975f;
    }

    @Override
    protected String getDefaultName() {
        return "Elder Guardian Ghost";
    }
}
