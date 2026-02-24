package org.cloudburstmc.api.inventory;

/**
 * A double-chest (54-slot, 6×9) virtual container screen with no backing block entities.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VirtualDoubleChestScreen view = player.createVirtualDoubleChest("Big Shop");
 * view.getStorage().setItem(0, ItemStack.builder(ItemTypes.DIAMOND).count(64).build());
 * player.openInventory(view);
 * }</pre>
 *
 * @see VirtualChestScreen
 * @see VirtualHopperScreen
 */
public interface VirtualDoubleChestScreen extends VirtualStorageScreen {
}
