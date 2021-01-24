package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Slime;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.Location;

/**
 * @author PikyCZ
 */
public class EntitySlime extends EntityHostile implements Slime {

    public EntitySlime(EntityType<Slime> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(16);
    }

    @Override
    public float getWidth() {
        return 2.04f;
    }

    @Override
    public float getHeight() {
        return 2.04f;
    }

    @Override
    public String getName() {
        return "Slime";
    }

    @Override
    public Item[] getDrops() {
        return new Item[]{Item.get(ItemIds.SLIME_BALL)};
    }
}
