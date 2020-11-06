package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import lombok.val;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.concurrent.Immutable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Immutable
public class FurnaceRecipe implements Recipe {

    private final ItemStack output;
    private final ItemStack ingredient;
    private final Identifier block;

    public FurnaceRecipe(ItemStack result, ItemStack ingredient, Identifier block) {
        this.output = result;
        this.ingredient = ingredient;
        this.block = block;
    }

    public ItemStack getInput() {
        return this.ingredient;
    }

    @Override
    public ItemStack getResult() {
        return this.output;
    }

    @Override
    public void registerToCraftingManager(CraftingManager manager) {
        manager.registerFurnaceRecipe(this);
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
    public CraftingData toNetwork(int netId) {
        val ingredientData = ((CloudItemStack) ingredient).getNetworkData();
        val outputData = ((CloudItemStack) output).getNetworkData();

        if (ingredientData.getDamage() >= 0) {
            return CraftingData.fromFurnaceData(ingredientData.getId(), ingredientData.getDamage(), outputData, block.getName(), netId);
        } else {
            return CraftingData.fromFurnace(ingredientData.getId(), outputData, block.getName(), netId);
        }
    }
}
