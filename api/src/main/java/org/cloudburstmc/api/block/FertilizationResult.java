package org.cloudburstmc.api.block;

import org.cloudburstmc.api.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Describes the changes and item drops produced by fertilizing a block.
 *
 * @param changes proposed block changes
 * @param drops   items to drop when the fertilization succeeds
 */
public record FertilizationResult(List<BlockChange> changes, List<ItemStack> drops) {

    private static final FertilizationResult NONE = new FertilizationResult(List.of(), List.of());

    public FertilizationResult {
        changes = List.copyOf(Objects.requireNonNull(changes, "changes"));
        drops = List.copyOf(Objects.requireNonNull(drops, "drops"));
    }

    /**
     * Gets a result representing an unsupported fertilization attempt.
     *
     * @return the empty result
     */
    public static FertilizationResult none() {
        return NONE;
    }

    /**
     * Checks whether fertilization produced an outcome.
     *
     * @return {@code true} when changes or drops were produced
     */
    public boolean succeeded() {
        return !this.changes.isEmpty() || !this.drops.isEmpty();
    }
}
