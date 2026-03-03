package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;

@FunctionalInterface
public interface InteractEntityHandler {

    boolean execute(Entity entity, Player player, ItemStack item, Vector3f clickedPos);
}
