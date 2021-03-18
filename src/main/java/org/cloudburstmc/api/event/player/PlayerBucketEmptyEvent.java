package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

public final class PlayerBucketEmptyEvent extends PlayerBucketEvent {

    public PlayerBucketEmptyEvent(Player who, Block blockStateClicked, Direction direction, ItemStack bucket, ItemStack itemInHand) {
        super(who, blockStateClicked.getState(), direction, bucket, itemInHand);
    }

}
