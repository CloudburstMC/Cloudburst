package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.ElderGuardian;
import org.cloudburstmc.api.level.Location;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.ELDER;

/**
 * @author PikyCZ
 */
public class EntityElderGuardian extends EntityHostile implements ElderGuardian {

    public EntityElderGuardian(EntityType<ElderGuardian> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(80);
        this.data.setFlag(ELDER, true);
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
    public String getName() {
        return "Elder Guardian";
    }
}
