package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * Fired when a recipe is being unlocked for a player for the first time.
 *
 * <p>Cancelling this event prevents the recipe from being added to the player's recipe book.
 * The recipe can be replaced with a different one by calling {@link #setRecipe(Recipe)}.
 */
public final class PlayerRecipeDiscoverEvent extends PlayerEvent implements Cancellable {

    private Recipe recipe;

    public PlayerRecipeDiscoverEvent(Player player, Recipe recipe) {
        super(player);
        this.recipe = recipe;
    }

    /**
     * Returns the recipe being unlocked.
     */
    public Recipe getRecipe() {
        return recipe;
    }

     /**
      * Replaces the recipe that will be unlocked with a different one.
      * Has no effect if the event is canceled.
      */
    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
