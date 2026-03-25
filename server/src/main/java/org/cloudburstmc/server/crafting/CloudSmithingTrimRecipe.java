package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.crafting.SmithingTrimRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;

public class CloudSmithingTrimRecipe implements SmithingTrimRecipe {
    private final Identifier id;
    private final ItemDescriptorWithCount template;
    private final ItemDescriptorWithCount base;
    private final ItemDescriptorWithCount addition;
    private final Identifier block;

    public CloudSmithingTrimRecipe(Identifier id, ItemDescriptorWithCount template, ItemDescriptorWithCount base, ItemDescriptorWithCount addition, Identifier block) {
        this.id = id;
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.block = block;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public ItemStack getResult() {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeType getType() {
        return RecipeType.SMITHING_TRIM;
    }

    @Override
    public Identifier getBlock() {
        return block;
    }

    public ItemDescriptorWithCount getTemplateDescriptor() {
        return template;
    }

    public ItemDescriptorWithCount getBaseDescriptor() {
        return base;
    }

    public ItemDescriptorWithCount getAdditionDescriptor() {
        return addition;
    }

    @Override
    public RecipeIngredient getTemplate() {
        return CloudShapedRecipe.descriptorToIngredient(template);
    }

    @Override
    public RecipeIngredient getBase() {
        return CloudShapedRecipe.descriptorToIngredient(base);
    }

    @Override
    public RecipeIngredient getAddition() {
        return CloudShapedRecipe.descriptorToIngredient(addition);
    }
}
