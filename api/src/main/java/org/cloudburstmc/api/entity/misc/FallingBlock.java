package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;

/**
 * Represents an entity carrying a block state while falling.
 */
public interface FallingBlock extends Entity {

    /**
     * Returns the block state carried by this entity.
     *
     * @return the carried block state
     */
    BlockState getBlockState();

    /**
     * Sets the block state carried by this entity.
     *
     * @param blockState the block state to carry
     */
    void setBlockState(BlockState blockState);

    /**
     * Returns whether this entity drops its block as an item when it cannot land.
     * A canceled drop prevents both placement and item drops regardless of this value.
     *
     * @return whether an item is dropped
     */
    boolean doesDropItem();

    /**
     * Sets whether this entity drops its block as an item when it cannot land.
     * A canceled drop prevents both placement and item drops regardless of this value.
     *
     * @param dropItem whether an item is dropped
     */
    void setDropItem(boolean dropItem);

    /**
     * Returns whether this entity disappears instead of placing or dropping its block.
     *
     * @return whether landing is canceled
     */
    boolean isDropCancelled();

    /**
     * Sets whether this entity disappears instead of placing or dropping its block.
     *
     * @param cancelDrop whether landing is canceled
     */
    void setDropCancelled(boolean cancelDrop);

    /**
     * Returns whether this entity damages entities when it lands.
     *
     * @return whether landing damage is enabled
     */
    boolean canHurtEntities();

    /**
     * Sets whether this entity damages entities when it lands.
     *
     * @param hurtEntities whether landing damage is enabled
     */
    void setHurtEntities(boolean hurtEntities);

    /**
     * Returns the landing damage applied per block fallen.
     *
     * @return damage per block
     */
    float getDamagePerBlock();

    /**
     * Sets the landing damage applied per block fallen.
     * A positive value also enables landing damage.
     *
     * @param damagePerBlock damage per block
     */
    void setDamagePerBlock(float damagePerBlock);

    /**
     * Returns the maximum landing damage.
     *
     * @return maximum landing damage
     */
    int getMaximumDamage();

    /**
     * Sets the maximum landing damage.
     *
     * @param maximumDamage maximum landing damage
     */
    void setMaximumDamage(int maximumDamage);

    /**
     * Returns whether this entity expires when falling too long or outside the level.
     *
     * @return whether automatic expiry is enabled
     */
    boolean doesAutoExpire();

    /**
     * Sets whether this entity expires when falling too long or outside the level.
     *
     * @param autoExpire whether automatic expiry is enabled
     */
    void setAutoExpire(boolean autoExpire);
}
