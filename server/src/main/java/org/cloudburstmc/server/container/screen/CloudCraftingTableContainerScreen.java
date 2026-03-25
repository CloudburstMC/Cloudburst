package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.inventory.CraftingTableScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.CraftingTableView;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudCraftingTableView;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudCraftingTableContainerScreen extends CloudBlockContainerScreen implements CraftingTableScreen {

    private CloudCraftingTableView craftingTableSection;
    private byte windowId;

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
        this.windowId = player.nextContainerId();

        Vector3i pos = block.getPosition();
        ContainerOpenPacket pkt = new ContainerOpenPacket();
        pkt.setId(this.windowId);
        pkt.setType(ContainerType.WORKBENCH);
        pkt.setBlockPosition(pos);
        player.sendPacket(pkt);

        player.registerUIContainer(craftingTableSection.getContainer(), ContainerSlotType.CRAFTING_INPUT, 32);
        player.getInventoryManager().sendAllInventories();
    }

    @Override
    public void close() {
        player.clearUIContainer();

        ContainerClosePacket pkt = new ContainerClosePacket();
        pkt.setId(this.windowId);
        pkt.setServerInitiated(true);
        pkt.setType(ContainerType.CONTAINER);
        player.sendPacket(pkt);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.craftingTableSection = new CloudCraftingTableView(new CloudContainer(9));
        this.addMapping(new ContainerMapping(ContainerSlotType.CRAFTING_INPUT, craftingTableSection, 9, -32));
    }
}
