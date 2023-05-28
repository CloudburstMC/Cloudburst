package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.player.Player;

public final class PlayerBedLeaveEvent extends PlayerEvent {

    private final Block bed;

    public PlayerBedLeaveEvent(Player player, Block bed) {
        super(player);
        this.bed = bed;
    }

    public Block getBed() {
        return bed;
    }
}
