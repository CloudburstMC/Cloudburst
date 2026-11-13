package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityAgeable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.entity.data.CloudCreatureData;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

/**
 * Abstract base class for hostile mobs. Extends {@link EntityCreature} and implements {@link EntityAgeable}
 * for baby/adult state.
 */
public abstract class EntityHostile extends EntityCreature implements EntityAgeable {

    public EntityHostile(EntityType<?> type, Location location) {
        this(type, location, new CloudCreatureData());
    }

    protected EntityHostile(EntityType<?> type, Location location, CloudCreatureData state) {
        super(type, location, state);
    }

    @Override
    public boolean isBaby() {
        return this.data.getFlag(BABY);
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        if (super.onInteract(player, item, clickedPos)) {
            return true;
        }

        if (item.getType() == ItemTypes.NAME_TAG) {
            if (item.get(ItemDataComponents.CUSTOM_NAME) != null) {
                this.setNameTag(item.get(ItemDataComponents.CUSTOM_NAME));
                this.setNameTagVisible(true);
                ((CloudPlayerInventory) player.getInventory()).getContainer().removeItem(item);
                return true;
            }
        }
        return false;
    }
}
