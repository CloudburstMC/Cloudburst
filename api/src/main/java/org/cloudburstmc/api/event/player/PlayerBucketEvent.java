package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

/**
 * Base event for a player filling or emptying a bucket.
 */
public abstract class PlayerBucketEvent extends PlayerEvent implements Cancellable {

    private final Block block;
    private final Block blockClicked;
    private final Direction blockFace;
    private final ItemStack bucket;
    private ItemStack item;

    /**
     * Creates a bucket event.
     *
     * @param player the player using the bucket
     * @param block the block changed by the operation
     * @param blockClicked the block the player clicked
     * @param blockFace the face of the clicked block
     * @param bucket the bucket used in the operation
     * @param item the item that will remain in the player's hand
     */
    protected PlayerBucketEvent(Player player, Block block, Block blockClicked, Direction blockFace,
                                ItemStack bucket, ItemStack item) {
        super(player);
        this.block = block;
        this.blockClicked = blockClicked;
        this.blockFace = blockFace;
        this.bucket = bucket;
        this.item = item;
    }

    /**
     * Returns the block changed by the bucket operation.
     *
     * @return the changed block
     */
    public Block getBlock() {
        return this.block;
    }

    /**
     * Returns the block clicked by the player.
     *
     * @return the clicked block
     */
    public Block getBlockClicked() {
        return this.blockClicked;
    }

    /**
     * Returns the face of the block clicked by the player.
     *
     * @return the clicked face
     */
    public Direction getBlockFace() {
        return this.blockFace;
    }

    /**
     * Returns the bucket used in the operation.
     *
     * @return the original bucket
     */
    public ItemStack getBucket() {
        return this.bucket;
    }

    /**
     * Returns the item that will remain in the player's hand.
     *
     * @return the resulting item
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * Sets the item that will remain in the player's hand.
     *
     * @param item the resulting item
     */
    public void setItem(ItemStack item) {
        this.item = item;
    }
}
