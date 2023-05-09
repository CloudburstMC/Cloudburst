package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.CraftingRecipe;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.FurnaceRecipeData;
import org.cloudburstmc.server.item.ItemUtils;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.List;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Immutable
public class FurnaceRecipe implements CraftingRecipe {

    private final Identifier recipeId;
    private final ItemStack output;
    private final ItemStack ingredient;
    private final Identifier block;

    public FurnaceRecipe(Identifier id, ItemStack result, ItemStack ingredient, Identifier block) {
        this.recipeId = id;
        this.output = result;
        this.ingredient = ingredient;
        this.block = block;
    }

    public ItemStack getInput() {
        return this.ingredient;
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
        return RecipeType.FURNACE;
    }

    @Override
    public Identifier getBlock() {
        return block;
    }

    @Override
    public boolean requiresCraftingTable() {
        return false;
    }

    @Override
    public List<ItemStack> getExtraResults() {
        return null;
    }

    @Override
    public List<ItemStack> getAllResults() {
        return Collections.singletonList(output);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    public FurnaceRecipeData toNetwork() {
        ItemData ingredientData = ItemUtils.toNetwork(ingredient);
        ItemData outputData = ItemUtils.toNetwork(output);

        if (ingredientData.getDamage() >= 0) {
            return FurnaceRecipeData.of(ingredientData.getDefinition().getRuntimeId(), ingredientData.getDamage(), outputData, block.getName());
        } else {
            return FurnaceRecipeData.of(ingredientData.getDefinition().getRuntimeId(), outputData, block.getName());
        }
    }

    @Override
    public boolean matchItems(ItemStack[][] input, ItemStack[][] output) {
        return false;
    }
}
