package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.CrafterScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockCrafterView;
import org.cloudburstmc.api.inventory.view.CreatedOutputView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.CrafterBlockEntity;
import org.cloudburstmc.server.container.ServerSlotGroupTypes;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the crafter block container.
 */
public class CloudCrafterContainerScreen extends CloudBlockContainerScreen implements CrafterScreen {

    private final CrafterBlockEntity crafterEntity;

    public CloudCrafterContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.CRAFTER, player, block);
        this.crafterEntity = getOrCreateBlockEntity(block, BlockEntityTypes.CRAFTER);
    }

    @Override
    protected Container getStorageContainer() {
        return crafterEntity.getContainer();
    }

    @Override
    public BlockCrafterView getCrafter() {
        return getSlotsOrThrow(SlotGroupTypes.CRAFTER);
    }

    @Override
    public CreatedOutputView getOutputSlot() {
        return getSlotsOrThrow(ServerSlotGroupTypes.CREATED_OUTPUT);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, crafterEntity));
    }
}
