package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import lombok.ToString;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

@ToString
public abstract class MixRecipe implements Recipe {

    private final ItemStack input;
    private final ItemStack ingredient;
    private final ItemStack output;

    public MixRecipe(ItemStack input, ItemStack ingredient, ItemStack output) {
        this.input = input.clone();
        this.ingredient = ingredient.clone();
        this.output = output.clone();
    }

    public ItemStack getIngredient() {
        return ingredient.clone();
    }

    public ItemStack getInput() {
        return input.clone();
    }

    public ItemStack getResult() {
        return output.clone();
    }

    @Override
    public RecipeType getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CraftingData toNetwork(int netId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identifier getBlock() {
        throw new UnsupportedOperationException();
    }
}