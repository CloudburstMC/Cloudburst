package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.WitherSkeleton;
import org.cloudburstmc.api.level.Location;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

/**
 * @author PikyCZ
 */
public class EntityWitherSkeleton extends EntityAbstractSkeleton implements WitherSkeleton {

    public EntityWitherSkeleton(EntityType<WitherSkeleton> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);

        this.fireProof = true;
        this.data.setFlag(FIRE_IMMUNE, true);
    }

    @Override
    public float getWidth() {
        return 0.7f;
    }

    @Override
    public float getHeight() {
        return 2.4f;
    }

    @Override
    protected String getDefaultName() {
        return "WitherSkeleton";
    }


}
