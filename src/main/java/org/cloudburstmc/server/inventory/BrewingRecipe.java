package org.cloudburstmc.server.inventory;


import com.nukkitx.protocol.bedrock.data.inventory.PotionMixData;
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
        return new PotionMixData(getInput().getNetworkId(), getInput().getMeta(), getIngredient().getNetworkId(), getIngredient().getMeta(), getResult().getNetworkId(), getResult().getMeta());
    }
}