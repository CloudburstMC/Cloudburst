package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public class PlayerBucketFillEvent extends PlayerBucketEvent {

    public PlayerBucketFillEvent(Player who, Block blockStateClicked, Direction direction, Item bucket, Item itemInHand) {
        super(who, blockStateClicked, direction, bucket, itemInHand);
    }

}
