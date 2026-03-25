package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.CookingRecipe;
import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.FurnaceRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.server.item.ItemUtils;

import javax.annotation.concurrent.Immutable;

@Immutable
public class CloudFurnaceRecipe implements CookingRecipe {

    private final Identifier recipeId;
    private final ItemStack output;
    private final ItemStack inputItem;
    private final ItemDescriptorWithCount inputDescriptor;
    private final Identifier block;
    private final int priority;

    public CloudFurnaceRecipe(Identifier id, ItemStack result, ItemStack inputItem, ItemDescriptorWithCount inputDescriptor, Identifier block, int priority) {
        this.recipeId = id;
        this.output = result;
        this.inputItem = inputItem;
        this.inputDescriptor = inputDescriptor;
        this.block = block;
        this.priority = priority;
    }

    @Override
    public RecipeIngredient getInput() {
        RecipeIngredient ingredient = CloudShapedRecipe.descriptorToIngredient(inputDescriptor);
        if (ingredient == null) {
            return new RecipeIngredient.Exact(inputItem.getType().getId());
        }
        return ingredient;
    }

    @Override
    public ItemStack getInputItem() {
        return this.inputItem;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public Identifier getId() {
        return this.recipeId;
    }

    @Override
    public ItemStack getResult() {
        return this.output;
    }

    @Override
    public RecipeType getType() {
        return RecipeType.COOKING;
    }

    @Override
    public Identifier getBlock() {
        return block;
    }

    public FurnaceRecipeData toNetwork() {
        ItemData ingredientData = ItemUtils.toNetwork(inputItem);
        ItemData outputData = ItemUtils.toNetwork(output);

        if (ingredientData.getDamage() >= 0) {
            return FurnaceRecipeData.of(ingredientData.getDefinition().getRuntimeId(), ingredientData.getDamage(), outputData, block.getName());
        } else {
            return FurnaceRecipeData.of(ingredientData.getDefinition().getRuntimeId(), outputData, block.getName());
        }
    }
}
