package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.inventory.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ShapelessRecipe implements CraftingRecipe {

    private final Identifier recipeId;
    private final ItemStack output;
    private final List<ItemStack> ingredients;
    private final int priority;
    private final Identifier block;
    private final List<ItemStack> extraOutputs = new ArrayList<>();

    public ShapelessRecipe(Identifier recipeId, int priority, List<ItemStack> outputs, List<ItemStack> ingredients, Identifier craftingBlock ) {
        this(recipeId, priority, outputs.remove(0),ingredients, craftingBlock);
        this.extraOutputs.addAll(outputs);
    }

    public ShapelessRecipe(Identifier recipeId, int priority, ItemStack result, List<ItemStack> ingredients, Identifier block) {
        this.recipeId = recipeId;
        this.priority = priority;
        this.output = result;
        this.block = block;
        if (ingredients.size() > 9) {
            throw new IllegalArgumentException("Shapeless recipes cannot have more than 9 ingredients");
        }

        this.ingredients = new ArrayList<>();

        for (ItemStack item : ingredients) {
            if (item.getAmount() < 1) {
                throw new IllegalArgumentException("Recipe '" + recipeId + "' Ingredient amount was not 1 (value: " + item.getAmount() + ")");
            }
            this.ingredients.add(item);
        }
    }

    @Override
    public Identifier getId() {
        return this.recipeId;
    }

    @Override
    public ItemStack getResult() {
        return this.output;
    }

    public List<ItemStack> getIngredientList() {
        return this.ingredients;
    }

    public int getIngredientCount() {
        return ingredients.size();
    }

    @Override
    public RecipeType getType() {
        return RecipeType.SHAPELESS;
    }

    @Override
    public boolean requiresCraftingTable() {
        return this.ingredients.size() > 4;
    }

    @Override
    public List<ItemStack> getExtraResults() {
        return this.extraOutputs;
    }

    @Override
    public List<ItemStack> getAllResults() {
        if(this.extraOutputs.size() == 0) {
            return Collections.singletonList(this.getResult());
        }
        List<ItemStack> list = new ArrayList<>();
        list.add(this.output);
        list.addAll(this.extraOutputs);
        return list;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean matchItems(ItemStack[][] input, ItemStack[][] output) {
        List haveInputs = new ArrayList<>();
        for (ItemStack[] items : input) {
            haveInputs.addAll(Arrays.asList(items));
        }
        haveInputs.sort(CloudRecipeRegistry.recipeComparator);

        List<ItemStack> needInputs = this.getIngredientList();

        if (!this.matchItemList(haveInputs, needInputs)) {
            return false;
        }

        List haveOutputs = new ArrayList<>();
        for (ItemStack[] items : output) {
            haveOutputs.addAll(Arrays.asList(items));
        }
        haveOutputs.sort(CloudRecipeRegistry.recipeComparator);
        List<ItemStack> needOutputs = this.getExtraResults();

        return this.matchItemList(haveOutputs, needOutputs);
    }


    private boolean matchItemList(List<ItemStack> haveItems, List<ItemStack> needItems) {
        // Remove any air blocks that may have gotten through.
        haveItems.removeIf(ItemStack::isNull);

        if (haveItems.size() != needItems.size()) {
            return false;
        }

        int size = needItems.size();
        int completed = 0;
        for (int i = 0; i < size; i++) {
            ItemStack haveItem = haveItems.get(i);
            ItemStack needItem = needItems.get(i);

            if (needItem.equals(haveItem)) {
                completed++;
            }
        }

        return completed == size;
    }

    @Override
    public Identifier getBlock() {
        return block;
    }
}
