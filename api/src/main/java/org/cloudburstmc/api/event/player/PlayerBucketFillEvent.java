package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

/**
 * Called when a player fills a bucket from a block.
 */
public final class PlayerBucketFillEvent extends PlayerBucketEvent {

    /**
     * Creates a bucket-fill event.
     *
     * @param player the player filling the bucket
     * @param block the block supplying the bucket contents
     * @param blockClicked the block the player clicked
     * @param blockFace the face of the clicked block
     * @param bucket the bucket used in the operation
     * @param item the item that will remain in the player's hand
     */
    public PlayerBucketFillEvent(Player player, Block block, Block blockClicked, Direction blockFace, ItemStack bucket, ItemStack item) {
        super(player, block, blockClicked, blockFace, bucket, item);
    }
}
