package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.player.Player;

@FunctionalInterface
public interface PlayerBlockHandler {

    void execute(Block block, Player player);
}
