package org.cloudburstmc.api.item.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.Tool;

/**
 * Obtains the mining behavior of an item.
 */
@FunctionalInterface
public interface GetToolHandler {

    /**
     * Returns the mining behavior carried by an item.
     *
     * @param item item being queried
     * @return tool behavior, or {@code null} when the item is not a tool
     */
    @Nullable
    Tool execute(ItemStack item);
}
