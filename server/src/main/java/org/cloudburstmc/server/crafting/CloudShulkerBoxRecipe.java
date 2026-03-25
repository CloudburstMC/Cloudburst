package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.ShulkerBoxRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;

import java.util.List;

public class CloudShulkerBoxRecipe extends CloudShapelessRecipe implements ShulkerBoxRecipe {

    public CloudShulkerBoxRecipe(Identifier recipeId, int priority, List<ItemStack> outputs, List<ItemStack> ingredients, List<ItemDescriptorWithCount> inputDescriptors, Identifier craftingBlock) {
        super(recipeId, priority, outputs, ingredients, inputDescriptors, craftingBlock, org.cloudburstmc.api.crafting.RecipeType.SHULKER_BOX);
    }

    @Override
    public RecipeIngredient getShulkerBox() {
        List<RecipeIngredient> choices = getIngredientChoices();
        if (choices.size() < 2) {
            throw new IllegalStateException("ShulkerBoxRecipe '" + getId() + "' has fewer than 2 ingredients");
        }
        return choices.getFirst();
    }

    @Override
    public RecipeIngredient getDye() {
        List<RecipeIngredient> choices = getIngredientChoices();
        if (choices.size() < 2) {
            throw new IllegalStateException("ShulkerBoxRecipe '" + getId() + "' has fewer than 2 ingredients");
        }
        return choices.get(1);
    }
}
