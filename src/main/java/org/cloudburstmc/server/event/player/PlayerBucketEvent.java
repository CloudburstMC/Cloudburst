package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public abstract class PlayerBucketEvent extends PlayerEvent implements Cancellable {

    private final Block blockStateClicked;

    private final Direction direction;

    private final ItemStack bucket;

    private ItemStack item;


    public PlayerBucketEvent(Player player, Block blockStateClicked, Direction direction, ItemStack bucket, ItemStack itemInHand) {
        super(player);
        this.blockStateClicked = blockStateClicked;
        this.direction = direction;
        this.item = itemInHand;
        this.bucket = bucket;
    }

    /**
     * Returns the bucket used in this event
     *
     * @return bucket
     */
    public ItemStack getBucket() {
        return this.bucket;
    }

    /**
     * Returns the item in hand after the event
     *
     * @return item
     */
    public ItemStack getItem() {
        return this.item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public Block getBlockClicked() {
        return this.blockStateClicked;
    }

    public Direction getBlockFace() {
        return this.direction;
    }
}
