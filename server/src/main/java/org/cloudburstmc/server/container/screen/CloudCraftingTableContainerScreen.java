package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.CraftingTableScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.CraftingTableView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ServerSlotGroupTypes;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudCraftingTableView;
import org.cloudburstmc.server.container.view.CloudSimpleSection;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the crafting table. All slots are ephemeral.
 */
public class CloudCraftingTableContainerScreen extends CloudBlockContainerScreen implements CraftingTableScreen {

    private CloudCraftingTableView craftingTableSection;
    private CloudSimpleSection craftingOutputSection;

    public CloudCraftingTableContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.CRAFTING_TABLE, player, block);
    }

    @Override
    public CraftingTableView getCraftingTable() {
        return craftingTableSection;
    }

    @Override
    protected Container getStorageContainer() {
        return craftingTableSection.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.craftingTableSection = new CloudCraftingTableView(new CloudContainer(9));
        this.craftingOutputSection = new CloudSimpleSection(ServerSlotGroupTypes.CREATED_OUTPUT, new CloudContainer(1));
        this.addMapping(new ContainerMapping(ContainerSlotType.CRAFTING_INPUT, craftingTableSection, 9, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.CRAFTING_OUTPUT, craftingOutputSection, 1, 0));
    }
}
