package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.DispenserScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockDispenserView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.DispenserBlockEntity;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the dispenser block container.
 */
public class CloudDispenserContainerScreen extends CloudBlockContainerScreen implements DispenserScreen {

    private final DispenserBlockEntity dispenserEntity;

    public CloudDispenserContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.DISPENSER, player, block);
        this.dispenserEntity = getOrCreateBlockEntity(block, BlockEntityTypes.DISPENSER);
    }

    @Override
    protected Container getStorageContainer() {
        return dispenserEntity.getContainer();
    }

    @Override
    public BlockDispenserView getDispenser() {
        return getSlotsOrThrow(SlotGroupTypes.DISPENSER);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, dispenserEntity));
    }
}
