package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerMixData;
import lombok.val;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;

public class ContainerRecipe extends MixRecipe {
    public ContainerRecipe(ItemStack input, ItemStack ingredient, ItemStack output) {
        super(input, ingredient, output);
    }

    @Override
    public void registerToCraftingManager(CraftingManager manager) {
        manager.registerContainerRecipe(this);
    }

    public ContainerMixData toData() {
        val inputData = ((CloudItemStack) getInput()).getNetworkData();
        val ingredientData = ((CloudItemStack) getIngredient()).getNetworkData();
        val resultData = ((CloudItemStack) getResult()).getNetworkData();
        return new ContainerMixData(inputData.getId(), ingredientData.getId(), resultData.getId());
    }
}
