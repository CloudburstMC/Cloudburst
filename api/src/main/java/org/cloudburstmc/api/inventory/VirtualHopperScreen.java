package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.HopperView;

/**
 * A hopper (5-slot, 1×5) virtual container screen with no backing block entity.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VirtualHopperScreen view = player.createVirtualHopper("Quick Menu");
 * view.getHopper().setItem(0, ItemStack.builder(ItemTypes.ARROW).amount(1).build());
 * player.openInventory(view);
 * }</pre>
 *
 * @see VirtualChestScreen
 * @see VirtualDoubleChestScreen
 */
public interface VirtualHopperScreen extends VirtualContainerScreen {

    /**
     * Returns the section containing the hopper's 5 item slots.
     *
     * @return the hopper slot group
     */
    HopperView getHopper();
}
