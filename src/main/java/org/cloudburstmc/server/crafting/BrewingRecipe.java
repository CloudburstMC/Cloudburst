package org.cloudburstmc.server.crafting;


import org.cloudburstmc.api.inventory.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;

public class BrewingRecipe extends MixRecipe {

    public BrewingRecipe(Identifier id, ItemStack input, ItemStack ingredient, ItemStack output) {
        super(id, input, ingredient, output);
    }

    @Override
    public RecipeType getType() {
        return RecipeType.POTION;
    }

    @Override
    public Identifier getBlock() {
        return Identifiers.BREWING_STAND;
    }
}