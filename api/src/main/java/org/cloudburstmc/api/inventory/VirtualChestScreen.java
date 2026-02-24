package org.cloudburstmc.api.inventory;

/**
 * A single-chest (27-slot, 3×9) virtual container screen with no backing block entity.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VirtualChestScreen view = player.createVirtualChest("Shop");
 * view.getStorage().setItem(0, ItemStack.builder(ItemTypes.DIAMOND).count(1).build());
 * player.openInventory(view);
 * }</pre>
 *
 * @see VirtualDoubleChestScreen
 * @see VirtualHopperScreen
 */
public interface VirtualChestScreen extends VirtualStorageScreen {
}
