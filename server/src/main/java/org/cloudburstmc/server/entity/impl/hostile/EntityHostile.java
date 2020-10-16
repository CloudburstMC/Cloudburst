package org.cloudburstmc.server.entity.impl.hostile;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.EntityCreature;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;

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
            if (item.hasName()) {
                this.setNameTag(item.getName());
                this.setNameTagVisible(true);
                player.getInventory().removeItem(item);
                return true;
            }
        }
        return false;
    }
}
