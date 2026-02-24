package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.DropperScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockDropperView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.DropperBlockEntity;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the dropper block container.
 */
public class CloudDropperContainerScreen extends CloudBlockContainerScreen implements DropperScreen {

    private final DropperBlockEntity dropperEntity;

    public CloudDropperContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.DROPPER, player, block);
        this.dropperEntity = getOrCreateBlockEntity(block, BlockEntityTypes.DROPPER);
    }

    @Override
    protected Container getStorageContainer() {
        return dropperEntity.getContainer();
    }

    @Override
    public BlockDropperView getDropper() {
        return getSlotsOrThrow(SlotGroupTypes.DROPPER);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, dropperEntity));
    }
}
