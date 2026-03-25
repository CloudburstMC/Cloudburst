package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

public class CloudContainerRecipe extends CloudMixRecipe {

    public CloudContainerRecipe(Identifier id, ItemStack input, ItemStack ingredient, ItemStack output) {
        super(id, input, ingredient, output);
    }

    @Override
    public RecipeType getType() {
        return RecipeType.POTION_CONTAINER;
    }
}
