package org.cloudburstmc.server.entity.impl.passive;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.entity.EntityAgeable;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.EntityCreature;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.BABY;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class Animal extends EntityCreature implements EntityAgeable {
    public Animal(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public boolean isBaby() {
        return this.data.getFlag(BABY);
    }

    public boolean isBreedingItem(ItemStack item) {
        return item.getType() == ItemTypes.WHEAT; //default
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
