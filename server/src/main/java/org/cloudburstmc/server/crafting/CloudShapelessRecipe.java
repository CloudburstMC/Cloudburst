package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.crafting.ShapelessRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CloudShapelessRecipe implements ShapelessRecipe {

    private final Identifier recipeId;
    private final ItemStack output;
    private final List<ItemStack> ingredients;
    private final List<ItemDescriptorWithCount> inputDescriptors;
    private final int priority;
    private final Identifier block;
    private final List<ItemStack> extraOutputs = new ArrayList<>();
    private final RecipeType type;

    public CloudShapelessRecipe(Identifier recipeId, int priority, List<ItemStack> outputs, List<ItemStack> ingredients, Identifier craftingBlock, RecipeType type) {
        this(recipeId, priority, outputs, ingredients, null, craftingBlock, type);
    }

    public CloudShapelessRecipe(Identifier recipeId, int priority, List<ItemStack> outputs, List<ItemStack> ingredients, List<ItemDescriptorWithCount> inputDescriptors, Identifier craftingBlock, RecipeType type) {
        this.output = outputs.removeFirst();
        this.extraOutputs.addAll(outputs);
        this.type = type;
        this.recipeId = recipeId;
        this.priority = priority;

        this.block = craftingBlock;
        if (ingredients.size() > 9) {
            throw new IllegalArgumentException("Shapeless recipes cannot have more than 9 ingredients");
        }

        this.ingredients = new ArrayList<>();
        for (ItemStack item : ingredients) {
            if (item.getCount() < 1) {
                throw new IllegalArgumentException("Recipe '" + recipeId + "' Ingredient amount was not >= 1 (value: " + item.getCount() + ")");
            }
            this.ingredients.add(item);
        }

        this.inputDescriptors = inputDescriptors;
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
    public List<ItemStack> getIngredientList() {
        return Collections.unmodifiableList(this.ingredients);
    }

    public List<ItemDescriptorWithCount> getInputDescriptors() {
        return this.inputDescriptors;
    }

    @Override
    public List<RecipeIngredient> getIngredientChoices() {
        if (inputDescriptors == null) return List.of();
        List<RecipeIngredient> choices = new ArrayList<>(inputDescriptors.size());
        for (ItemDescriptorWithCount descriptor : inputDescriptors) {
            choices.add(CloudShapedRecipe.descriptorToIngredient(descriptor));
        }
        return Collections.unmodifiableList(choices);
    }

    @Override
    public int getIngredientCount() {
        return ingredients.size();
    }

    @Override
    public RecipeType getType() {
        return this.type;
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
        if (this.extraOutputs.isEmpty()) {
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

    public boolean matchItems(ItemStack[][] input, ItemStack[][] output) {
        List<ItemStack> haveInputs = new ArrayList<>();
        for (ItemStack[] items : input) {
            haveInputs.addAll(Arrays.asList(items));
        }
        haveInputs.sort(CloudRecipeRegistry.recipeComparator);

        List<ItemStack> needInputs = this.getIngredientList();

        if (!this.matchItemList(haveInputs, needInputs)) {
            return false;
        }

        List<ItemStack> haveOutputs = new ArrayList<>();
        for (ItemStack[] items : output) {
            haveOutputs.addAll(Arrays.asList(items));
        }
        haveOutputs.sort(CloudRecipeRegistry.recipeComparator);
        List<ItemStack> needOutputs = this.getExtraResults();

        return this.matchItemList(haveOutputs, needOutputs);
    }

    private boolean matchItemList(List<ItemStack> haveItems, List<ItemStack> needItems) {
        haveItems.removeIf(ItemStack::isEmpty);

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
