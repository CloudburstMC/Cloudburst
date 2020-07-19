package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;

public class PlayerBucketEmptyEvent extends PlayerBucketEvent {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public PlayerBucketEmptyEvent(Player who, BlockState blockStateClicked, BlockFace blockFace, Item bucket, Item itemInHand) {
        super(who, blockStateClicked, blockFace, bucket, itemInHand);
    }

}
