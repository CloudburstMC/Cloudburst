package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Strider;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

public class EntityStrider extends Animal implements Strider {

    public EntityStrider(EntityType<Strider> entityType, Location location) {
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
        return this.isBaby() ? 0.45f : 0.9f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.85f : 1.7f;
    }

    @Override
    protected String getDefaultName() {
        return "Strider";
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        return item.getType() == ItemTypes.WARPED_FUNGUS;
    }
}
