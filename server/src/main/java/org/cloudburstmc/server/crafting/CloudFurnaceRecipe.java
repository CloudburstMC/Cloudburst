package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.CookingRecipe;
import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.server.item.ItemUtils;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.UUID;

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

    public ShapelessRecipeData toNetwork(UUID uuid, int netId) {
        return ShapelessRecipeData.shapeless(
                recipeId.toString(),
                List.of(inputDescriptor),
                ItemUtils.toNetworkRecipe(List.of(output)),
                uuid,
                block.getName(),
                priority,
                netId,
                new RecipeUnlockingRequirement(RecipeUnlockingRequirement.UnlockingContext.ALWAYS_UNLOCKED));
    }
}
