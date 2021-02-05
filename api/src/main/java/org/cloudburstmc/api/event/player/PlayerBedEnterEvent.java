package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public class PlayerBedEnterEvent extends PlayerEvent implements Cancellable {

    private final Block bed;

    public PlayerBedEnterEvent(Player player, Block bed) {
        super(player);
        this.bed = bed;
    }

    public Block getBed() {
        return bed;
    }
}
