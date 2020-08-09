package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import lombok.ToString;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.Identifier;

@ToString
public abstract class MixRecipe implements Recipe {

    private final Item input;
    private final Item ingredient;
    private final Item output;

    public MixRecipe(Item input, Item ingredient, Item output) {
        this.input = input.clone();
        this.ingredient = ingredient.clone();
        this.output = output.clone();
    }

    public Item getIngredient() {
        return ingredient.clone();
    }

    public Item getInput() {
        return input.clone();
    }

    public Item getResult() {
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