package org.cloudburstmc.api.event.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import static java.util.Objects.requireNonNull;

/**
 * Fired when a player finishes consuming food or a drink, before its effects or inventory change.
 * Cancelling leaves the item untouched. A replacement overrides the item normally left in the used slot.
 * Changing {@link #getItem()} selects the item whose consumption effects and normal result are used.
 */
public class PlayerItemConsumeEvent extends PlayerEvent implements Cancellable {

    private ItemStack item;
    private @Nullable ItemStack replacement;

    /**
     * @param player the player consuming the item
     * @param item   the item before consumption
     */
    public PlayerItemConsumeEvent(Player player, ItemStack item) {
        super(player);
        this.item = requireNonNull(item, "item");
    }

    /**
     * @return the item whose consumption effects will be applied
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * Changes the item whose consumption effects and normal result are used.
     * Use {@link ItemStack#EMPTY} to consume no item. This is separate from {@link #setReplacement(ItemStack)}.
     *
     * @param item the item to consume
     */
    public void setItem(ItemStack item) {
        this.item = requireNonNull(item, "item");
    }

    /**
     * @return the item to leave in the used slot, or {@code null} for the normal result
     */
    public @Nullable ItemStack getReplacement() {
        return this.replacement;
    }

    /**
     * Overrides the item left in the used slot after consumption. Pass {@code null} to use the normal result.
     *
     * @param replacement replacement item, or {@code null}
     */
    public void setReplacement(@Nullable ItemStack replacement) {
        this.replacement = replacement;
    }
}
