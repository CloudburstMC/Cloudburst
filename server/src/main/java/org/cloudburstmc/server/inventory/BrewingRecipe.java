package org.cloudburstmc.server.inventory;


import com.nukkitx.protocol.bedrock.data.inventory.PotionMixData;
import lombok.val;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;


public class BrewingRecipe extends MixRecipe {

    public BrewingRecipe(ItemStack input, ItemStack ingredient, ItemStack output) {
        super(input, ingredient, output);
    }

    @Override
    public void registerToCraftingManager(CraftingManager manager) {
        manager.registerBrewingRecipe(this);
    }

    public PotionMixData toData() {
        val inputData = ((CloudItemStack) getInput()).getNetworkData();
        val ingredientData = ((CloudItemStack) getIngredient()).getNetworkData();
        val resultData = ((CloudItemStack) getResult()).getNetworkData();

        return new PotionMixData(inputData.getId(), inputData.getDamage(), ingredientData.getId(), ingredientData.getDamage(), resultData.getId(), resultData.getDamage());
    }
}