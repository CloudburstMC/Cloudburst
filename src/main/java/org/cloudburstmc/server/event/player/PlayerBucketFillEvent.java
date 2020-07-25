package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public class PlayerBucketFillEvent extends PlayerBucketEvent {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public PlayerBucketFillEvent(Player who, BlockState blockStateClicked, Direction direction, Item bucket, Item itemInHand) {
        super(who, blockStateClicked, direction, bucket, itemInHand);
    }

}
