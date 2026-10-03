package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.inventory.CraftingTableScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.CraftingTableView;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudCraftingTableView;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudCraftingTableContainerScreen extends CloudBlockContainerScreen implements CraftingTableScreen {

    private CloudCraftingTableView craftingTableSection;

    public CloudCraftingTableContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.CRAFTING_TABLE, player, block);
    }

    @Override
    public CraftingTableView getCraftingTable() {
        return craftingTableSection;
    }

    @Override
    protected Container getStorageContainer() {
        throw new UnsupportedOperationException("Crafting table has no storage container");
    }

    @Override
    public void open() {
        byte windowId = player.nextContainerId();

        Vector3i pos = block.getPosition();
        ContainerOpenPacket pkt = new ContainerOpenPacket();
        pkt.setId(windowId);
        pkt.setType(ContainerType.WORKBENCH);
        pkt.setBlockPosition(pos);
        this.openWindow(pkt);

        player.registerUIContainer(craftingTableSection.getContainer(), ContainerSlotType.CRAFTING_INPUT, 32);
        player.getInventoryManager().sendAllInventories();
    }

    @Override
    public void close() {
        player.clearUIContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.craftingTableSection = new CloudCraftingTableView(new CloudContainer(9));
        this.addMapping(new ContainerMapping(ContainerSlotType.CRAFTING_INPUT, craftingTableSection, 9, -32));
    }
}
