package org.cloudburstmc.api.event.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Fired before a player's food level or saturation changes. Cancelling prevents the change.
 * Accepted values are clamped independently to the range from 0 to 20.
 */
public class PlayerFoodLevelChangeEvent extends PlayerEvent implements Cancellable {

    private int foodLevel;
    private float saturation;
    private final @Nullable ItemStack item;

    /**
     * @param player     the affected player
     * @param foodLevel  proposed food level
     * @param saturation proposed saturation
     * @param item       consumed item, or {@code null} for another cause
     */
    public PlayerFoodLevelChangeEvent(Player player, int foodLevel, float saturation, @Nullable ItemStack item) {
        super(player);
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.item = item;
    }

    /**
     * @return the consumed item, or {@code null} for other causes
     */
    public @Nullable ItemStack getItem() {
        return this.item;
    }

    /**
     * @return the proposed food level
     */
    public int getFoodLevel() {
        return this.foodLevel;
    }

    /**
     * @param foodLevel the proposed food level
     */
    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    /**
     * @return the proposed saturation level
     */
    public float getSaturation() {
        return this.saturation;
    }

    /**
     * @param saturation the proposed saturation level
     */
    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }
}
