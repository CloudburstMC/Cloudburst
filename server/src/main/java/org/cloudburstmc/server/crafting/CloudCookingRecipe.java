package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.CookingRecipe;
import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTagKey;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.server.item.RecipeItemCodec;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.UUID;

@Immutable
public class CloudCookingRecipe implements CookingRecipe {

    private final Identifier recipeId;
    private final ItemStack output;
    private final ItemStack inputItem;
    private final ItemDescriptorWithCount inputDescriptor;
    private final Identifier block;
    private final int priority;
    private final int cookingTime;
    private final float experience;
    private final RecipeIngredient ingredient;

    public CloudCookingRecipe(
            Identifier id, ItemStack result, ItemStack inputItem, ItemDescriptorWithCount inputDescriptor,
            Identifier block, int priority, int cookingTime, float experience) {
        if (cookingTime < 1 || !Float.isFinite(experience) || experience < 0) {
            throw new IllegalArgumentException("Invalid cooking time or experience for " + id);
        }

        this.recipeId = id;
        this.output = result;
        this.inputItem = inputItem;
        this.inputDescriptor = inputDescriptor;
        this.block = block;
        this.priority = priority;
        this.cookingTime = cookingTime;
        this.experience = experience;

        RecipeIngredient resolved = CloudShapedRecipe.descriptorToIngredient(inputDescriptor);
        this.ingredient = resolved == null ? new RecipeIngredient.Exact(inputItem.getType().getId()) : resolved;
    }

    @Override
    public RecipeIngredient getInput() {
        return this.ingredient;
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
    public int getCookingTime() {
        return this.cookingTime;
    }

    @Override
    public float getExperience() {
        return this.experience;
    }

    public int getInputCount() {
        return Math.max(1, this.inputDescriptor.getCount());
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() < this.getInputCount()) {
            return false;
        }

        CloudItemRegistry items = CloudItemRegistry.get();
        if (this.ingredient instanceof RecipeIngredient.Tag(Identifier tag)) {
            return items.getTag(ItemTagKey.of(tag)).isTagged(stack.getType());
        }

        if (!this.ingredient.test(stack)) {
            return false;
        }

        return !(this.inputDescriptor.getDescriptor() instanceof DefaultDescriptor descriptor)
                || descriptor.getAuxValue() == Short.MAX_VALUE
                || items.getSerializer(stack.getType()).getAuxValue(stack) == descriptor.getAuxValue();
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
                RecipeItemCodec.toNetworkRecipe(List.of(output)),
                uuid,
                block.getName(),
                priority,
                netId,
                new RecipeUnlockingRequirement(RecipeUnlockingRequirement.UnlockingContext.ALWAYS_UNLOCKED));
    }
}
