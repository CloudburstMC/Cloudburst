package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Nautilus;
import org.cloudburstmc.api.level.Location;

public class EntityNautilus extends EntityAbstractNautilus implements Nautilus {

    public EntityNautilus(EntityType<Nautilus> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.44f : super.getWidth();
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.5f : super.getHeight();
    }

    @Override
    protected String getDefaultName() {
        return "Nautilus";
    }
}
