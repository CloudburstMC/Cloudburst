package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Fired when a player takes a smelted item from a furnace result slot.
 *
 * <p>Set the experience awarded via {@link #setExperience(int)}.
 * The default is 0.
 */
public final class FurnaceExtractEvent extends Event {

    private final Player player;
    private final Furnace furnace;
    private final ItemStack item;
    private int experience;

    public FurnaceExtractEvent(Player player, Furnace furnace, ItemStack item, int experience) {
        this.player = player;
        this.furnace = furnace;
        this.item = item;
        this.experience = experience;
    }

    /**
     * Returns the player who took the item from the result slot.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the furnace the item was taken from.
     */
    public Furnace getFurnace() {
        return furnace;
    }

    /**
     * Returns the item that was extracted from the result slot.
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the amount of experience that will be awarded to the player.
     * Defaults to 0 unless changed via {@link #setExperience(int)}.
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Overrides the experience awarded to the player for this extraction.
     *
     * @param experience the new XP amount
     */
    public void setExperience(int experience) {
        this.experience = experience;
    }
}
