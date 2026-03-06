package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.inventory.view.CraftingView;
import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;

/**
 * Represents the player's own inventory screen (the 2×2 crafting grid, armor slots,
 * offhand slot, and main inventory that the player sees when they open their own inventory.
 *
 * <p>This screen is NOT a {@link ContainerScreen}: it is the player's own inventory, not a
 * container being opened alongside the player's inventory. As a result it does not expose
 * {@code getPlayerInventory()} (there is no "container" half) and extends
 * {@link InventoryScreen} directly.</p>
 *
 * <p>Like {@link ContainerScreen}, this screen implements {@link CursorAccess}; plugins
 * can use {@code instanceof CursorAccess} to access the cursor without knowing the concrete
 * screen type:</p>
 * <pre>{@code
 * if (screen instanceof CursorAccess ca) {
 *     ItemStack held = ca.getCursor().getCursor();
 * }
 * }</pre>
 *
 * @see ContainerScreen
 * @see CursorAccess
 */
public interface PlayerInventoryScreen extends InventoryScreen, CursorAccess {

    /**
     * Returns the player's main 36-slot inventory view.
     *
     * @return the player inventory view
     */
    PlayerInventoryView getInventory();

    /**
     * Returns the player's 2×2 crafting grid.
     *
     * @return the crafting view
     */
    CraftingView getCraftingGrid();

    /**
     * Returns the player's armor slots view.
     *
     * @return the armor view
     */
    ArmorView getArmor();

    /**
     * Returns the player's offhand slot view.
     *
     * @return the offhand view
     */
    OffhandView getOffhand();
}
