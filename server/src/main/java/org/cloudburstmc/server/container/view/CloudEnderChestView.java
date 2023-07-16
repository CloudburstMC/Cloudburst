package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.container.view.EnderChestView;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudEnderChestView extends CloudPlayerContainerView implements EnderChestView {

    private final Block block;

    public CloudEnderChestView(CloudPlayer holder, Block block) {
        super(ContainerViewTypes.ENDER_CHEST, holder, holder.getEnderChest());
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return block;
    }
}
