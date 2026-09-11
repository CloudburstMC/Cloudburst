package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.crafting.MixRecipe;
import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.SmithingUpgrade;
import org.cloudburstmc.api.item.TrimMaterial;
import org.cloudburstmc.api.item.TrimPattern;
import org.cloudburstmc.api.util.Identifier;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Registry for all crafting, cooking, brewing, smithing, and stonecutting recipes.
 * Supports registering custom recipes, querying existing ones, and removing recipes by identifier.
 */
public interface RecipeRegistry extends KeyedRegistry<Recipe> {

    /**
     * Registers a recipe. Throws {@link RegistryException} if a recipe with the same identifier
     * is already registered.
     */
    void register(Recipe recipe) throws RegistryException;

    /**
     * Loads and registers all recipes from the given file. The file must be in the standard
     * recipes JSON format.
     */
    void loadFromFile(URI file);

    /**
     * Returns the recipe with the given identifier, or {@code null} if none is registered.
     */
    Recipe getRecipe(Identifier identifier);

    @Override
    default Optional<Recipe> get(Identifier id) {
        return Optional.ofNullable(this.getRecipe(id));
    }

    @Override
    default Identifier getId(Recipe value) {
        return value.getId();
    }

    /**
     * Returns the recipe with the given UUID, or {@code null} if none is registered.
     */
    Recipe getRecipe(UUID uuid);

    /**
     * Finds a crafting recipe matching the given input grid, expected output, and extra outputs
     * at the specified crafting station. Returns {@code null} if no match is found.
     */
    Recipe matchRecipe(ItemStack[][] inputMap, ItemStack output, ItemStack[][] extraOutputMap, Identifier craftingBlock);

    /**
     * Finds the first recipe at the given station whose input matches the provided item stack.
     * Intended for single-input stations such as furnaces and the stonecutter.
     * Returns {@code null} if no matching recipe is found.
     */
    Recipe matchRecipe(ItemStack input, Identifier craftingBlock);

    /**
     * Returns all registered recipes.
     */
    Collection<Recipe> getRecipes();

    @Override
    default Collection<Recipe> values() {
        return this.getRecipes();
    }

    /**
     * Returns all registered recipes of the given type.
     */
    Collection<Recipe> getRecipes(RecipeType type);

    /**
     * Returns all registered recipes whose crafting station matches the given block identifier.
     */
    Collection<Recipe> getRecipesByStation(Identifier craftingBlock);

    /**
     * Returns all recipes at the given station that accept the provided item as input.
     * Useful for showing every possible output for a given input at a stonecutter or furnace.
     * Returns an empty collection if no matching recipes are found.
     */
    Collection<Recipe> getRecipesForInput(ItemStack input, Identifier craftingBlock);

    /**
     * Removes the recipe with the given identifier from the registry.
     * Has no effect if no recipe with that identifier is registered.
     */
    void unregister(Identifier id);

    /**
     * Removes a recipe from the registry.
     * Has no effect if the recipe is not registered.
     */
    void unregister(Recipe recipe);

    /**
     * Registers a trim pattern for use in smithing trim recipes.
     */
    void registerTrimPattern(TrimPattern pattern);

    /**
     * Registers a trim material for use in smithing trim recipes.
     */
    void registerTrimMaterial(TrimMaterial material);

    /**
     * Returns all registered trim patterns.
     */
    Collection<TrimPattern> getTrimPatterns();

    /**
     * Returns all registered trim materials.
     */
    Collection<TrimMaterial> getTrimMaterials();

    /**
     * Registers a netherite smithing upgrade pairing.
     */
    void registerSmithingUpgrade(SmithingUpgrade upgrade);

    /**
     * Returns all registered netherite smithing upgrade pairings.
     */
    Collection<SmithingUpgrade> getSmithingUpgrades();

    /**
     * Returns all registered recipes whose primary result matches the given item by type and metadata,
     * ignoring stack size.
     */
    Collection<Recipe> getRecipesFor(ItemStack result);

    /**
     * Finds the first brewing or container mix recipe that matches the given ingredient and base bottle.
     * Returns {@code null} if no match exists.
     *
     * @param ingredient the item placed in the ingredient slot
     * @param basePotion the potion bottle being transformed
     */
    MixRecipe matchBrewingRecipe(ItemStack ingredient, ItemStack basePotion);
}
