package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * Created by Snake1999 on 2016/1/22.
 * Package cn.nukkit.event.block in project nukkit.
 */
public final class DoorToggleEvent extends BlockUpdateEvent implements Cancellable {

    private Player player;

    public DoorToggleEvent(Block block, Player player) {
        super(block);
        this.player = player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
