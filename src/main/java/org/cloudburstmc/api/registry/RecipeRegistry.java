package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.inventory.Recipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

import java.net.URI;
import java.util.Collection;

public interface RecipeRegistry extends Registry {

    void register(Recipe recipe) throws RegistryException;

    void loadFromFile(URI file);

    Recipe getRecipe(Identifier identifier);

    Recipe matchRecipe(ItemStack[][] inputMap, ItemStack output, ItemStack[][] extraOutputMap, Identifier craftingBlock);

    Collection<Recipe> getRecipes();

    void unregister(Identifier id);

    /**
     * Used to remove a {@link Recipe} from the Registry.
     *
     * @param recipe instance of the recipe to remove (i.e. as returned by {@link RecipeRegistry#matchRecipe(ItemStack[][], ItemStack, ItemStack[][], Identifier)}
     */
    void unregister(Recipe recipe);
}
