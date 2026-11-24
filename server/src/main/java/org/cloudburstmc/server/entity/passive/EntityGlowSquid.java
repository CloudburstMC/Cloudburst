package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.GlowSquid;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

public class EntityGlowSquid extends EntitySquid implements GlowSquid {

    public EntityGlowSquid(EntityType<GlowSquid> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.475f : 0.95f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.475f : 0.95f;
    }

    @Override
    protected String getDefaultName() {
        return "Glow Squid";
    }

    @Override
    protected ItemType getInkSacType() {
        return ItemTypes.GLOW_INK_SAC;
    }
}
