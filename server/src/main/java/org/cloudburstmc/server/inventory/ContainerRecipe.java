package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerMixData;
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
        return new ContainerMixData(getInput().getNetworkId(), getIngredient().getNetworkId(), getResult().getNetworkId());
    }
}
