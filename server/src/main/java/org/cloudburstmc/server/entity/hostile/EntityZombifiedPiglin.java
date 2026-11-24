package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.ZombifiedPiglin;
import org.cloudburstmc.api.level.Location;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

public class EntityZombifiedPiglin extends EntityZombie implements ZombifiedPiglin {

    public EntityZombifiedPiglin(EntityType<ZombifiedPiglin> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.fireProof = true;
        this.data.setFlag(FIRE_IMMUNE, true);
    }

    @Override
    protected String getDefaultName() {
        return "Zombified Piglin";
    }
}
