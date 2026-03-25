package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

public class CloudBrewingRecipe extends CloudMixRecipe {

    public CloudBrewingRecipe(Identifier id, ItemStack input, ItemStack ingredient, ItemStack output) {
        super(id, input, ingredient, output);
    }

    @Override
    public RecipeType getType() {
        return RecipeType.POTION;
    }
}
