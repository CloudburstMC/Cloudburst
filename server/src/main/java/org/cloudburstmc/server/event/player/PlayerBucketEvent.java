package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;

public abstract class PlayerBucketEvent extends PlayerEvent implements Cancellable {

    private final BlockState blockStateClicked;

    private final BlockFace blockFace;

    private final Item bucket;

    private Item item;


    public PlayerBucketEvent(Player player, BlockState blockStateClicked, BlockFace blockFace, Item bucket, Item itemInHand) {
        super(player);
        this.blockStateClicked = blockStateClicked;
        this.blockFace = blockFace;
        this.item = itemInHand;
        this.bucket = bucket;
    }

    /**
     * Returns the bucket used in this event
     *
     * @return bucket
     */
    public Item getBucket() {
        return this.bucket;
    }

    /**
     * Returns the item in hand after the event
     *
     * @return item
     */
    public Item getItem() {
        return this.item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public BlockState getBlockClicked() {
        return this.blockStateClicked;
    }

    public BlockFace getBlockFace() {
        return this.blockFace;
    }
}
