package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.inventory.BrewingStandScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockBrewingStandView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.BrewingStandBlockEntity;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudBrewingStandView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the brewing stand block container.
 */
public class CloudBrewingContainerScreen extends CloudBlockContainerScreen implements BrewingStandScreen {

    private final BrewingStandBlockEntity brewingEntity;
    private final CloudBrewingStandView brewingStandView;

    public CloudBrewingContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.BREWING_STAND, player, block);
        this.brewingEntity = getOrCreateBlockEntity(block, BlockEntityTypes.BREWING_STAND);
        this.brewingStandView = new CloudBrewingStandView(this.brewingEntity);
    }

    @Override
    protected Container getStorageContainer() {
        return brewingEntity.getContainer();
    }

    @Override
    public BlockBrewingStandView getBrewingStand() {
        return this.brewingStandView;
    }

    @Override
    public void open() {
        if (this.brewingEntity.hasCustomName()) {
            this.brewingEntity.spawnTo(this.player);
        }
        super.open();
        this.brewingEntity.sendProgress(this.player);
    }

    public boolean acceptsItem(ContainerSlotType type, int slot, ItemStack item) {
        return switch (type) {
            case BREWING_INPUT, BREWING_RESULT, BREWING_FUEL -> this.brewingEntity.acceptsItem(this.resolveInventorySlot(type, slot), item);
            default -> true;
        };
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.BREWING_INPUT, this.brewingStandView, BrewingStandBlockEntity.INGREDIENT_SLOT, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.BREWING_RESULT, this.brewingStandView, BrewingStandBlockEntity.FIRST_BOTTLE_SLOT, BrewingStandBlockEntity.BOTTLE_COUNT, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.BREWING_FUEL, this.brewingStandView, BrewingStandBlockEntity.FUEL_SLOT, 1, 0));
    }
}
