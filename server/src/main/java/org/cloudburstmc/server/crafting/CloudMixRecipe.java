package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.MixRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;

abstract class CloudMixRecipe implements MixRecipe {

    private final Identifier id;
    private final ItemStack input;
    private final ItemStack ingredient;
    private final ItemStack output;

    CloudMixRecipe(Identifier id, ItemStack input, ItemStack ingredient, ItemStack output) {
        this.id = id;
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public ItemStack getInput() {
        return input;
    }

    @Override
    public ItemStack getIngredient() {
        return ingredient;
    }

    @Override
    public ItemStack getResult() {
        return output;
    }

    @Override
    public Identifier getBlock() {
        return Identifiers.BREWING_STAND;
    }
}
