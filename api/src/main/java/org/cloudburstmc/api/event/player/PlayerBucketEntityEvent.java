package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.entity.Bucketable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Called when a player captures an entity in a bucket.
 */
public final class PlayerBucketEntityEvent extends PlayerEvent implements Cancellable {

    private final Bucketable entity;
    private final ItemStack originalBucket;
    private final ItemStack entityBucket;

    /**
     * Creates a bucket entity event.
     *
     * @param player the player capturing the entity
     * @param entity the entity being captured
     * @param originalBucket the bucket used to capture the entity
     * @param entityBucket the bucket containing the captured entity
     */
    public PlayerBucketEntityEvent(Player player, Bucketable entity, ItemStack originalBucket, ItemStack entityBucket) {
        super(player);
        this.entity = entity;
        this.originalBucket = originalBucket;
        this.entityBucket = entityBucket;
    }

    /**
     * Returns the entity being captured.
     *
     * @return the captured entity
     */
    public Bucketable getEntity() {
        return this.entity;
    }

    /**
     * Returns the bucket used to capture the entity.
     *
     * @return the original bucket
     */
    public ItemStack getOriginalBucket() {
        return this.originalBucket;
    }

    /**
     * Returns the bucket containing the captured entity.
     *
     * @return the entity bucket
     */
    public ItemStack getEntityBucket() {
        return this.entityBucket;
    }
}
