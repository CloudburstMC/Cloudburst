package org.cloudburstmc.server.container;

import org.cloudburstmc.api.crafting.MixRecipe;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;

public class ContainerRecipe extends MixRecipe {
    public ContainerRecipe(Identifier id, ItemStack input, ItemStack ingredient, ItemStack output) {
        super(id, input, ingredient, output);
    }

    @Override
    public RecipeType getType() {
        return RecipeType.CONTAINER;
    }

    @Override
    public Identifier getBlock() {
        return Identifiers.BREWING_STAND;
    }
}
