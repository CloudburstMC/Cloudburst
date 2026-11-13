package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called before a player places or removes a plant in a flower pot.
 * Cancelling the event prevents both the pot change and the item transfer.
 */
public class PlayerFlowerPotManipulateEvent extends PlayerEvent implements Cancellable {

    private final Block flowerpot;
    private final ItemStack item;
    private final boolean placing;

    /**
     * @param player    the player interacting with the pot
     * @param flowerpot the flower pot being changed
     * @param item      the plant item being placed or taken
     * @param placing   whether the player is placing the plant
     */
    public PlayerFlowerPotManipulateEvent(Player player, Block flowerpot, ItemStack item, boolean placing) {
        super(player);
        this.flowerpot = Objects.requireNonNull(flowerpot, "flowerpot");
        this.item = Objects.requireNonNull(item, "item");
        this.placing = placing;
    }

    /**
     * @return the flower pot being changed
     */
    public Block getFlowerpot() {
        return this.flowerpot;
    }

    /**
     * @return the single plant item being placed or removed
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * @return whether the player is placing the plant rather than taking it
     */
    public boolean isPlacing() {
        return this.placing;
    }
}
