package org.cloudburstmc.server.crafting;

import lombok.ToString;
import org.cloudburstmc.api.inventory.Recipe;
import org.cloudburstmc.api.inventory.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

@ToString
public abstract class MixRecipe implements Recipe {

    private final ItemStack input;
    private final ItemStack ingredient;
    private final ItemStack output;
    private final Identifier recipeId;

    public MixRecipe(Identifier id, ItemStack input, ItemStack ingredient, ItemStack output) {
        this.recipeId = id;
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
    }

    @Override
    public Identifier getId() {
        return this.recipeId;
    }

    public ItemStack getIngredient() {
        return ingredient;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getResult() {
        return output;
    }

    @Override
    public RecipeType getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identifier getBlock() {
        throw new UnsupportedOperationException();
    }
}