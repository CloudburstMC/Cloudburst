package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * Handles a player interacting with an entity.
 */
@FunctionalInterface
public interface InteractEntityHandler {

    /**
     * @param entity the entity being interacted with
     * @param player the interacting player
     * @param item the item held by the player
     * @param clickedPos the clicked position relative to the entity
     * @return whether the interaction was handled
     */
    boolean execute(Entity entity, Player player, ItemStack item, Vector3f clickedPos);
}
