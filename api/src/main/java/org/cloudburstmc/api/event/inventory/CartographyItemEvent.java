package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.inventory.CartographyScreen;
import org.cloudburstmc.api.inventory.view.CartographyView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Fired before a player takes a cartography result. Cancelling leaves both inputs unchanged.
 */
public class CartographyItemEvent extends InventoryClickEvent {

    /**
     * @param screen           the cartography screen
     * @param source           the prepared result
     * @param destination      the item already in the destination slot
     * @param click            the click gesture
     * @param destinationSlot  the destination's inventory slot
     * @param destinationGroup the destination slot group
     * @param result           the proposed destination item
     */
    public CartographyItemEvent(CartographyScreen screen, ItemStack source, ItemStack destination, ClickType click, int destinationSlot, SlotGroup destinationGroup, ItemStack result) {
        super(screen, 2, screen.getCartography(), source, destination, ActionType.TAKE, click, destinationSlot, destinationGroup, result);
    }

    /**
     * @return the cartography table's input and result slots
     */
    public CartographyView getInventory() {
        return ((CartographyScreen) this.getScreen()).getCartography();
    }
}
