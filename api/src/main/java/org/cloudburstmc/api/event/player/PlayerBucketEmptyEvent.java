package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

/**
 * Called when a player empties a bucket into the level.
 */
public final class PlayerBucketEmptyEvent extends PlayerBucketEvent {

    /**
     * Creates a bucket-empty event.
     *
     * @param player the player emptying the bucket
     * @param block the block receiving the bucket contents
     * @param blockClicked the block the player clicked
     * @param blockFace the face of the clicked block
     * @param bucket the bucket used in the operation
     * @param item the item that will remain in the player's hand
     */
    public PlayerBucketEmptyEvent(Player player, Block block, Block blockClicked, Direction blockFace, ItemStack bucket, ItemStack item) {
        super(player, block, blockClicked, blockFace, bucket, item);
    }
}
