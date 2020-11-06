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
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
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
    public CraftingData toNetwork(int netId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identifier getBlock() {
        throw new UnsupportedOperationException();
    }
}