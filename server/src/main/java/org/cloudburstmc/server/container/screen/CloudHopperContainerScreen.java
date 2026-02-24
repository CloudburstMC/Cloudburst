package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.HopperScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockHopperView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.HopperBlockEntity;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the hopper block container.
 */
public class CloudHopperContainerScreen extends CloudBlockContainerScreen implements HopperScreen {

    private final HopperBlockEntity hopperEntity;

    public CloudHopperContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.HOPPER, player, block);
        this.hopperEntity = getOrCreateBlockEntity(block, BlockEntityTypes.HOPPER);
    }

    @Override
    protected Container getStorageContainer() {
        return hopperEntity.getContainer();
    }

    @Override
    public BlockHopperView getHopper() {
        return getSlotsOrThrow(SlotGroupTypes.HOPPER);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, hopperEntity));
    }
}
