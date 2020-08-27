package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public class PlayerBucketEmptyEvent extends PlayerBucketEvent {

    public PlayerBucketEmptyEvent(Player who, Block blockStateClicked, Direction direction, Item bucket, Item itemInHand) {
        super(who, blockStateClicked, direction, bucket, itemInHand);
    }

}
