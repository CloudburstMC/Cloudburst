package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface UseBlockBehavior {

    boolean use(Behavior<Executor> behavior, Block block, Player player, Direction direction);

    @FunctionalInterface
    interface Executor {
        boolean execute(Block block, Player player, Direction direction);
    }
}
