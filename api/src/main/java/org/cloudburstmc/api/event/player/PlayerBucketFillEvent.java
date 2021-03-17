package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

public final class PlayerBucketFillEvent extends PlayerBucketEvent {

    public PlayerBucketFillEvent(Player who, BlockState
            blockStateClicked, Direction direction, ItemStack bucket, ItemStack itemInHand) {
        super(who, blockStateClicked, direction, bucket, itemInHand);
    }

}
