package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;

public class SmithingTransformRecipe implements Recipe {
    private final Identifier id;
    private final ItemDescriptorWithCount template;
    private final ItemDescriptorWithCount base;
    private final ItemDescriptorWithCount addition;
    private final ItemStack result;
    private final Identifier block;

    public SmithingTransformRecipe(Identifier id, ItemDescriptorWithCount template,
                                   ItemDescriptorWithCount base, ItemDescriptorWithCount addition,
                                   ItemStack result, Identifier block) {
        this.id = id;
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
        this.block = block;
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
        return RecipeType.SMITHING_TRANSFORM;
    }

    @Override
    public Identifier getBlock() {
        return block;
    }

    public ItemDescriptorWithCount getTemplate() {
        return template;
    }

    public ItemDescriptorWithCount getBase() {
        return base;
    }

    public ItemDescriptorWithCount getAddition() {
        return addition;
    }
}
