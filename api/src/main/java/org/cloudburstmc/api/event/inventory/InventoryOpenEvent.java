package org.cloudburstmc.api.event.inventory;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.inventory.InventoryScreen;

/**
 * Called when a player opens an inventory screen.
 *
 * <p>Plugins may call {@link #setTitleOverride(String)} to replace the title shown
 * to the player without modifying the underlying screen.</p>
 */
public final class InventoryOpenEvent extends InventoryEvent implements Cancellable {

    @Nullable
    private String titleOverride;

    public InventoryOpenEvent(InventoryScreen screen) {
        super(screen);
    }

    /**
     * Returns the title override set by a plugin, or {@code null} if no override
     * has been set and the screen's default title will be used.
     *
     * @return the title override, or {@code null}
     */
    @Nullable
    public String getTitleOverride() {
        return titleOverride;
    }

    /**
     * Overrides the title shown to the player when this screen opens.
     * Pass {@code null} to clear any existing override and use the default title.
     *
     * @param title the title to display, or {@code null} to clear the override
     */
    public void setTitleOverride(@Nullable String title) {
        this.titleOverride = title;
    }
}
