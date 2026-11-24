package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.WanderingTrader;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudMob;
import org.cloudburstmc.server.entity.component.MobNameTagInteraction;

public class EntityWanderingTrader extends CloudMob implements WanderingTrader {

    public EntityWanderingTrader(EntityType<WanderingTrader> type, Location location) {
        super(type, location);
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return MobNameTagInteraction.tryApply(this, player, item);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.8f;
    }

    @Override
    protected String getDefaultName() {
        return "Wandering Trader";
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
    }
}
