package org.cloudburstmc.api.crafting;

/**
 * Controls when a crafting recipe is made visible to players in the recipe book.
 *
 * <p>The default for all built-in recipes is {@link #ALWAYS_UNLOCKED}. Use
 * {@link #PLAYER_IN_WATER} or {@link #PLAYER_HAS_MANY_ITEMS} for recipes that should only
 * appear when specific conditions are met, or {@link #NONE} to suppress automatic unlocking
 * entirely and handle discovery manually.
 */
public enum RecipeUnlockContext {
    /**
     * The recipe is never automatically unlocked. Discovery must be handled explicitly.
     */
    NONE,
    /**
     * The recipe is always visible in the recipe book without any condition.
     */
    ALWAYS_UNLOCKED,
    /**
     * The recipe becomes visible when the player enters water.
     */
    PLAYER_IN_WATER,
    /**
     * The recipe becomes visible when the player has collected many different item types.
     */
    PLAYER_HAS_MANY_ITEMS
}
