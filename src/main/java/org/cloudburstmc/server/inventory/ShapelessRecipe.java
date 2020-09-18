package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.utils.Identifier;

import java.util.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ShapelessRecipe implements CraftingRecipe {

    private final String recipeId;
    private final ItemStack output;
    private final List<ItemStack> ingredients;
    private final int priority;
    private final Identifier block;

    private UUID id;

    public ShapelessRecipe(String recipeId, int priority, ItemStack result, Collection<ItemStack> ingredients, Identifier block) {
        this.recipeId = recipeId;
        this.priority = priority;
        this.output = result;
        this.block = block;
        if (ingredients.size() > 9) {
            throw new IllegalArgumentException("Shapeless recipes cannot have more than 9 ingredients");
        }

        this.ingredients = new ArrayList<>();

        for (ItemStack item : ingredients) {
            if (item.getCount() < 1) {
                throw new IllegalArgumentException("Recipe '" + recipeId + "' Ingredient amount was not 1 (value: " + item.getCount() + ")");
            }
            this.ingredients.add(item);
        }
    }

    @Override
    public ItemStack getResult() {
        return this.output;
    }

    @Override
    public String getRecipeId() {
        return this.recipeId;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public List<ItemStack> getIngredientList() {
        return this.ingredients;
    }

    public int getIngredientCount() {
        return ingredients.size();
    }

    @Override
    public void registerToCraftingManager(CraftingManager manager) {
        manager.registerShapelessRecipe(this);
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
        return new ArrayList<>();
    }

    @Override
    public List<ItemStack> getAllResults() {
        return Collections.singletonList(this.getResult());
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public boolean matchItems(ItemStack[][] input, ItemStack[][] output) {
        List<ItemStack> haveInputs = new ArrayList<>();
        for (ItemStack[] items : input) {
            haveInputs.addAll(Arrays.asList(items));
        }
        haveInputs.sort(CraftingManager.recipeComparator);

        List<ItemStack> needInputs = this.getIngredientList();

        if (!this.matchItemList(haveInputs, needInputs)) {
            return false;
        }

        List<ItemStack> haveOutputs = new ArrayList<>();
        for (ItemStack[] items : output) {
            haveOutputs.addAll(Arrays.asList(items));
        }
        haveOutputs.sort(CraftingManager.recipeComparator);
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

    @Override
    public CraftingData toNetwork(int netId) {
        return CraftingData.fromShapeless(this.recipeId, ItemUtils.toNetwork(this.getIngredientList()),
                ItemUtils.toNetwork(this.getAllResults()), this.id, this.block.getName(), this.priority, netId);
    }
}
