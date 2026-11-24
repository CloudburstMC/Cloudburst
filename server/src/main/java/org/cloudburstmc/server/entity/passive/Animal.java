package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudMob;
import org.cloudburstmc.server.entity.component.MobNameTagInteraction;
import org.cloudburstmc.server.entity.data.CloudMobData;

public abstract class Animal extends CloudMob {
    public Animal(EntityType<?> type, Location location) {
        this(type, location, new CloudMobData());
    }

    protected Animal(EntityType<?> type, Location location, CloudMobData state) {
        super(type, location, state);
    }

    public boolean isBreedingItem(ItemStack item) {
        return item.getType() == ItemTypes.WHEAT; //default
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return super.interactDefault(player, item, clickedPos) || MobNameTagInteraction.tryApply(this, player, item);
    }
}
