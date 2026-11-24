package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Enemy;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudMob;
import org.cloudburstmc.server.entity.component.MobNameTagInteraction;
import org.cloudburstmc.server.entity.data.CloudMobData;

public abstract class EntityHostile extends CloudMob implements Enemy {

    public EntityHostile(EntityType<?> type, Location location) {
        this(type, location, new CloudMobData());
    }

    protected EntityHostile(EntityType<?> type, Location location, CloudMobData state) {
        super(type, location, state);
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return super.interactDefault(player, item, clickedPos) || MobNameTagInteraction.tryApply(this, player, item);
    }
}
