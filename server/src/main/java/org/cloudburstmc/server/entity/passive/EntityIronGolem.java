package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.IronGolem;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudMob;
import org.cloudburstmc.server.entity.component.MobNameTagInteraction;

public class EntityIronGolem extends CloudMob implements IronGolem {

    public EntityIronGolem(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(100);
        this.setHealth(100);
    }

    @Override
    public float getWidth() {
        return 1.4f;
    }

    @Override
    public float getLength() {
        return this.getWidth();
    }

    @Override
    public float getHeight() {
        return 2.7f;
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return MobNameTagInteraction.tryApply(this, player, item);
    }
}
