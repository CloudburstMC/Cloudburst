package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.crafting.StonecuttingRecipe;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.inventory.StonecutterScreen;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called when a player requests a different stonecutting recipe.
 */
public class PlayerStonecutterRecipeSelectEvent extends PlayerEvent implements Cancellable {

    private final StonecutterScreen screen;
    private StonecuttingRecipe recipe;

    public PlayerStonecutterRecipeSelectEvent(Player player, StonecutterScreen screen, StonecuttingRecipe recipe) {
        super(player);
        this.screen = Objects.requireNonNull(screen, "screen");
        this.recipe = Objects.requireNonNull(recipe, "recipe");
    }

    /**
     * Returns the screen where the recipe was selected.
     */
    public StonecutterScreen getScreen() {
        return this.screen;
    }

    /**
     * Returns the proposed recipe.
     */
    public StonecuttingRecipe getRecipe() {
        return this.recipe;
    }

    /**
     * Replaces the proposed recipe. The replacement must be registered and accept the current input.
     *
     * @param recipe the replacement recipe
     */
    public void setRecipe(StonecuttingRecipe recipe) {
        this.recipe = Objects.requireNonNull(recipe, "recipe");
    }
}
