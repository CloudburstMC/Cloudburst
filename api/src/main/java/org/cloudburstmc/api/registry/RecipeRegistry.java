package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.inventory.Recipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

import java.nio.file.Path;
import java.util.Collection;

public interface RecipeRegistry extends Registry {

    void register(Identifier identifier, Recipe recipe) throws RegistryException;

    void loadFromFile(Path file);

    Recipe getRecipe(Identifier identifier);

    Recipe matchRecipe(ItemStack[][] inputMap, ItemStack output, ItemStack[][] extraOutputMap);

    Collection<Recipe> getRecipes();
}
