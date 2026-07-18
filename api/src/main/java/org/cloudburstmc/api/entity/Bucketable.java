package org.cloudburstmc.api.entity;

import org.cloudburstmc.api.item.ItemStack;

/**
 * An entity that can be captured and released using a bucket.
 */
public interface Bucketable extends Entity {

    /**
     * Returns whether this entity originated from a bucket.
     *
     * @return {@code true} if this entity originated from a bucket
     */
    boolean isFromBucket();

    /**
     * Sets whether this entity originated from a bucket.
     *
     * @param fromBucket whether this entity originated from a bucket
     */
    void setFromBucket(boolean fromBucket);

    /**
     * Returns this entity's base bucket item.
     *
     * @return the base bucket item
     */
    ItemStack getBaseBucketItem();
}
