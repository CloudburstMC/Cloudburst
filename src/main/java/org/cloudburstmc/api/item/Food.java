package org.cloudburstmc.api.item;

import org.cloudburstmc.api.player.Player;

public interface Food {

    boolean onEatenBy(Player player);
}
