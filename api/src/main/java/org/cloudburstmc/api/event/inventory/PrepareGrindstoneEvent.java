package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.inventory.GrindstoneScreen;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Fired when the result of a grindstone is recalculated.
 */
public class PrepareGrindstoneEvent extends PrepareResultEvent {

    /**
     * @param screen the grindstone screen
     * @param result the calculated result, or an empty stack
     */
    public PrepareGrindstoneEvent(GrindstoneScreen screen, ItemStack result) {
        super(screen, result);
    }

    @Override
    public GrindstoneScreen getScreen() {
        return (GrindstoneScreen) super.getScreen();
    }
}
