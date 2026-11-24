package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Zoglin;
import org.cloudburstmc.api.level.Location;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

public class EntityZoglin extends EntityHostile implements Zoglin {

    public EntityZoglin(EntityType<Zoglin> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(40);
        this.fireProof = true;
        this.data.setFlag(FIRE_IMMUNE, true);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.6982f : 1.3965f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.7f : 1.4f;
    }

    @Override
    protected String getDefaultName() {
        return "Zoglin";
    }
}
