package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.crafting.StonecuttingRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;

public record CloudStonecuttingRecipe(
        Identifier id,
        int priority,
        ItemStack inputItem,
        ItemDescriptorWithCount inputDescriptor,
        ItemStack result
) implements StonecuttingRecipe {

    @Override
    public RecipeIngredient getInput() {
        return CloudShapedRecipe.descriptorToIngredient(inputDescriptor);
    }

    @Override
    public ItemStack getInputItem() {
        return inputItem;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public ItemStack getResult() {
        return result;
    }

    @Override
    public RecipeType getType() {
        return RecipeType.STONECUTTING;
    }

    @Override
    public Identifier getBlock() {
        return Identifiers.STONECUTTER;
    }

    @Override
    public int getPriority() {
        return priority;
    }
}
