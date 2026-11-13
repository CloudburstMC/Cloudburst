package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.inventory.CraftingTableScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.CraftingTableView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudCraftingTableView;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudCraftingTableContainerScreen extends CloudBlockContainerScreen implements CraftingTableScreen {

    private final CloudCraftingTableView craftingTableSection = new CloudCraftingTableView(new CloudContainer(9));

    public CloudCraftingTableContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.CRAFTING_TABLE, player, block);
    }

    @Override
    public CraftingTableView getCraftingTable() {
        return this.craftingTableSection;
    }

    @Override
    protected Container getStorageContainer() {
        return this.craftingTableSection.getContainer();
    }

    @Override
    public void close() {
        for (int slot = 0; slot < this.craftingTableSection.size(); slot++) {
            ItemStack item = this.craftingTableSection.getItem(slot);
            if (item.isEmpty()) {
                continue;
            }

            this.craftingTableSection.setItem(slot, ItemStack.EMPTY);
            for (ItemStack remaining : this.player.getContainer().addItem(item)) {
                this.player.dropItem(remaining);
            }
        }
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new UIContainerMapping(ContainerSlotType.CRAFTING_INPUT, this.craftingTableSection));
    }
}
