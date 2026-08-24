package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

/**
 * Base interface for all recipes. Every recipe has a unique identifier, a result item,
 * a type that categorizes it, and an associated crafting station.
 */
public interface Recipe {

    /**
     * Returns the unique identifier for this recipe.
     */
    Identifier getId();

    /**
     * Returns the item produced when this recipe is completed.
     */
    ItemStack getResult();

    /**
     * Returns the type of this recipe, indicating which crafting mechanic it belongs to.
     */
    RecipeType getType();

    /**
     * Returns the identifier of the crafting station this recipe belongs to, or {@code null}
     * if no station is associated.
     *
     * <p>Common values by recipe category:
     * <ul>
     *   <li>Crafting recipes ({@link ShapedRecipe}, {@link ShapelessRecipe}): {@code minecraft:crafting_table}
     *       for 3x3 recipes, or a specific block identifier for station-specific recipes.
     *   <li>Cooking recipes ({@link CookingRecipe}): {@code minecraft:furnace}, {@code minecraft:blast_furnace},
     *       {@code minecraft:smoker}, or {@code minecraft:campfire}.
     *   <li>Smithing recipes ({@link SmithingRecipe}): {@code minecraft:smithing_table}.
     *   <li>Stonecutting recipes ({@link StonecuttingRecipe}): {@code minecraft:stonecutter}.
     *   <li>Brewing recipes ({@link MixRecipe}): {@code minecraft:brewing_stand}.
     *   <li>Complex recipes ({@link ComplexRecipe}): {@link BlockTypes#AIR}, no physical station.
     * </ul>
     */
    Identifier getBlock();
}
