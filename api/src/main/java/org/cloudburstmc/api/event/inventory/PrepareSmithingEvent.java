package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.crafting.SmithingRecipe;
import org.cloudburstmc.api.inventory.SmithingScreen;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Fired when the current smithing inputs match a recipe. Changing the preview
 * does not change which ingredients are consumed when the result is taken.
 */
public class PrepareSmithingEvent extends PrepareResultEvent {

    private final SmithingRecipe recipe;

    /**
     * @param screen the smithing table screen
     * @param result the recipe output
     * @param recipe the matched recipe
     */
    public PrepareSmithingEvent(SmithingScreen screen, ItemStack result, SmithingRecipe recipe) {
        super(screen, result);
        this.recipe = Objects.requireNonNull(recipe, "recipe");
    }

    @Override
    public SmithingScreen getScreen() {
        return (SmithingScreen) super.getScreen();
    }

    /**
     * @return the recipe matched by the current inputs
     */
    public SmithingRecipe getRecipe() {
        return this.recipe;
    }
}
