package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

import static java.util.Objects.requireNonNull;

/**
 * The item state and whether active use ends after a tick.
 *
 * @param item      the updated item
 * @param stopUsing whether the holder should stop using it
 */
public record UseTickResult(ItemStack item, boolean stopUsing) {

    public UseTickResult {
        requireNonNull(item, "item");
    }

    public static UseTickResult continueUsing(ItemStack item) {
        return new UseTickResult(item, false);
    }

    public static UseTickResult stopUsing(ItemStack item) {
        return new UseTickResult(item, true);
    }
}
