package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.EntityCreature;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityHostile extends EntityCreature {

    public EntityHostile(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        if (item.getType() == ItemTypes.NAME_TAG) {
            if (item.get(ItemKeys.CUSTOM_NAME) != null) {
                this.setNameTag(item.get(ItemKeys.CUSTOM_NAME));
                this.setNameTagVisible(true);
                player.getInventory().removeItem(item);
                return true;
            }
        }
        return false;
    }
}
