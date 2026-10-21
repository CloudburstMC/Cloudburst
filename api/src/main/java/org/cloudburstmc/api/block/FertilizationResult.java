package org.cloudburstmc.api.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.TreeType;

import java.util.List;
import java.util.Objects;

/**
 * Describes a fertilization attempt without applying its changes or spawning its drops.
 * The changes and drops are copied into immutable lists.
 *
 * @param changes   proposed block changes
 * @param drops     items to drop when the fertilization succeeds
 * @param succeeded whether fertilizer is accepted, including attempts without block changes
 * @param treeType  the proposed tree form, or {@code null} for other fertilization
 */
public record FertilizationResult(List<BlockChange> changes, List<ItemStack> drops, boolean succeeded, @Nullable TreeType treeType) {

    private static final FertilizationResult NONE = new FertilizationResult(List.of(), List.of(), false, null);
    private static final FertilizationResult UNCHANGED = new FertilizationResult(List.of(), List.of(), true, null);

    /**
     * @throws IllegalArgumentException if a rejected attempt specifies changes, drops or a tree form
     */
    public FertilizationResult {
        changes = List.copyOf(Objects.requireNonNull(changes, "changes"));
        drops = List.copyOf(Objects.requireNonNull(drops, "drops"));

        if (!succeeded && (!changes.isEmpty() || !drops.isEmpty() || treeType != null)) {
            throw new IllegalArgumentException("Unsupported fertilization cannot produce an outcome");
        }
    }

    /**
     * Creates a result accepted when it contains changes or drops.
     *
     * @param changes proposed block changes
     * @param drops   items to drop
     */
    public FertilizationResult(List<BlockChange> changes, List<ItemStack> drops) {
        this(changes, drops, !changes.isEmpty() || !drops.isEmpty(), null);
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
     * Returns an accepted attempt without proposed block changes or item drops.
     *
     * @return the accepted no-change result
     */
    public static FertilizationResult unchanged() {
        return UNCHANGED;
    }
}
