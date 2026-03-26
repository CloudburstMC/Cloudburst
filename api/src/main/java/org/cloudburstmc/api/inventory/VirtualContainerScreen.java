package org.cloudburstmc.api.inventory;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Common base for all virtual container screens that have no
 * backing block entity in the world.
 *
 * <p>Concrete subtypes differ only in slot count and the phantom block used:</p>
 * <ul>
 *   <li>{@link VirtualChestScreen}: 27 slots, single chest, use {@link VirtualStorageScreen#getStorage()}</li>
 *   <li>{@link VirtualDoubleChestScreen}: 54 slots, double chest, use {@link VirtualStorageScreen#getStorage()}</li>
 *   <li>{@link VirtualHopperScreen}: 5 slots, hopper, use {@link VirtualHopperScreen#getHopper()}</li>
 * </ul>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * // 1. Obtain a virtual screen from the player (server creates it lazily per player)
 * VirtualChestScreen chest = player.createVirtualChestScreen();
 * chest.setTitle(Component.text("My Shop"));
 *
 * // 2. Populate items BEFORE opening; modifying slots while the screen is open causes a desync.
 * StorageView storage = chest.getStorage();
 * storage.setItem(0, ItemStack.builder(ItemTypes.DIAMOND).amount(1).build());
 *
 * // 3. Open the screen for the player.
 * player.openInventory(chest);
 *
 * // 4. Listen for InventoryClickEvent to respond to clicks, checking
 * //    event.getScreen() == chest to identify your screen.
 * }</pre>
 */
public interface VirtualContainerScreen extends ContainerScreen {

    /**
     * Returns {@code true} if this screen is currently open for the player.
     *
     * @return {@code true} if open
     */
    boolean isOpen();

    /**
     * Returns the title displayed in the GUI title bar.
     *
     * @return the current title (never {@code null})
     */
    @NonNull
    Component getTitle();

    /**
     * Sets the title displayed in the GUI title bar.
     *
     * <p>The title is stored immediately but only sent to the client when the screen is next
     * opened via {@link org.cloudburstmc.api.player.Player#openInventory(InventoryScreen)}.
     * If the screen is currently open, the new title will not be visible until the screen is
     * closed and reopened.</p>
     *
     * <p>To override the title at open time (including for non-virtual containers), handle
     * {@link org.cloudburstmc.api.event.inventory.InventoryOpenEvent} and call
     * {@code event.setTitleOverride(newTitle)} instead.</p>
     *
     * @param title the title to display when the screen is next opened
     */
    void setTitle(Component title);
}

