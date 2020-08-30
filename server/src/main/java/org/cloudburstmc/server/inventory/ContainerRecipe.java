package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerMixData;
import org.cloudburstmc.server.item.behavior.Item;

public class ContainerRecipe extends MixRecipe {
    public ContainerRecipe(Item input, Item ingredient, Item output) {
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
