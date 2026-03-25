package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.crafting.StonecuttingRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;

import java.util.List;

public class CloudStonecuttingRecipe implements StonecuttingRecipe {

    private static final Identifier STONECUTTER = Identifier.parse("stonecutter");

    private final Identifier id;
    private final ItemStack inputItem;
    private final ItemDescriptorWithCount inputDescriptor;
    private final ItemStack result;
    private final int priority;

    public CloudStonecuttingRecipe(Identifier id, int priority, ItemStack inputItem, ItemDescriptorWithCount inputDescriptor, ItemStack result) {
        this.id = id;
        this.priority = priority;
        this.inputItem = inputItem;
        this.inputDescriptor = inputDescriptor;
        this.result = result;
    }

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
        return STONECUTTER;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public List<ItemDescriptorWithCount> getInputDescriptors() {
        return List.of(inputDescriptor);
    }
}
