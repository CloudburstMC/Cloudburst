package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.inventory.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.server.crafting.MixRecipe;

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
