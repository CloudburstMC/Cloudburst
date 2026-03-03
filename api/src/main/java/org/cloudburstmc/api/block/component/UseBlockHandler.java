package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

@FunctionalInterface
public interface UseBlockHandler {

    boolean execute(Block block, Player player, Direction direction);
}
