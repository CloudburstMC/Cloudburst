package org.cloudburstmc.server.crafting;

import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import lombok.val;
import org.cloudburstmc.api.inventory.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.item.CloudItemStack;

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
        return ((CloudItemStack) ingredient).getNetworkData().getDamage() >= 0 ? RecipeType.FURNACE_DATA : RecipeType.FURNACE;
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

    public CraftingData toNetwork(int netId) {
        val ingredientData = ((CloudItemStack) ingredient).getNetworkData();
        val outputData = ((CloudItemStack) output).getNetworkData();

        if (ingredientData.getDamage() >= 0) {
            return CraftingData.fromFurnaceData(ingredientData.getId(), ingredientData.getDamage(), outputData, block.getName(), netId);
        } else {
            return CraftingData.fromFurnace(ingredientData.getId(), outputData, block.getName(), netId);
        }
    }

    @Override
    public boolean matchItems(ItemStack[][] input, ItemStack[][] output) {
        return false;
    }
}
