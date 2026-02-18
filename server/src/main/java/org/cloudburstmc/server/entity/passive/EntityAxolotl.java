package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Axolotl;
import org.cloudburstmc.api.level.Location;

public class EntityAxolotl extends Animal implements Axolotl {

    public EntityAxolotl(EntityType<Axolotl> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(14);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.375f : 0.75f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.21f : 0.42f;
    }

    @Override
    public String getName() {
        return "Axolotl";
    }
}
