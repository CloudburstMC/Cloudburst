package org.cloudburstmc.server.entity.passive;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.entity.EntityAgeable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.entity.EntityCreature;

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
