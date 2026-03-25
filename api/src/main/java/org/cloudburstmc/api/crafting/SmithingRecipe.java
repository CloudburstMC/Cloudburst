package org.cloudburstmc.api.crafting;

/**
 * A recipe processed at the smithing table with three distinct input slots:
 * a template, a base item, and an addition material.
 *
 * <p>Each slot is described by a {@link RecipeIngredient}, which can be a specific
 * item ({@link RecipeIngredient.Exact}) or any item carrying a given tag
 * ({@link RecipeIngredient.Tag}).
 */
public interface SmithingRecipe extends Recipe {

    /**
     * Returns the smithing template item required in the template slot.
     */
    RecipeIngredient getTemplate();

    /**
     * Returns the base item to be upgraded or trimmed.
     */
    RecipeIngredient getBase();

    /**
     * Returns the material or reagent placed in the addition slot.
     */
    RecipeIngredient getAddition();
}
