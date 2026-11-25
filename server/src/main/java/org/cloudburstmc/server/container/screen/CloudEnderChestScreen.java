package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.inventory.EnderChestScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.EnderChestView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.block.component.ChestBlockHandlers;
import org.cloudburstmc.server.blockentity.EnderChestBlockEntity;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudEnderChestView;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class CloudEnderChestScreen extends CloudBlockContainerScreen implements EnderChestScreen {

    private final EnderChestBlockEntity chest;
    private final CloudEnderChestView enderChest;
    private boolean opened;

    private CloudEnderChestScreen(CloudPlayer player, Block block, EnderChestBlockEntity chest) {
        super(ScreenTypes.ENDER_CHEST, player, block);
        this.chest = chest;
        this.enderChest = new CloudEnderChestView(player);
    }

    public static @Nullable CloudEnderChestScreen create(CloudPlayer player, Block block) {
        BlockEntity entity = block.getLevel().getBlockEntity(block.getPosition());
        if (entity == null) {
            ChestBlockHandlers.ENDER_CHEST_ADDED.execute(block);
            entity = block.getLevel().getBlockEntity(block.getPosition());
        }

        if (!(entity instanceof EnderChestBlockEntity chest) || chest.isBlocked()) {
            return null;
        }

        return new CloudEnderChestScreen(player, block, chest);
    }

    @Override
    public EnderChestBlockEntity getChest() {
        return this.chest;
    }

    @Override
    public EnderChestView getEnderChest() {
        return this.getSlotsOrThrow(SlotGroupTypes.ENDER_CHEST);
    }

    @Override
    public boolean canOpen() {
        return this.canKeepOpen() && !this.chest.isBlocked();
    }

    @Override
    public boolean canKeepOpen() {
        return !this.chest.isClosed() && this.isWithinReach();
    }

    @Override
    public void open() {
        super.open();
        this.opened = true;
        this.chest.startOpen(this.player);
    }

    @Override
    public void close() {
        if (this.opened) {
            this.opened = false;
            this.chest.stopOpen(this.player);
        }

        super.close();
    }

    @Override
    protected Container getStorageContainer() {
        return this.enderChest.getContainer();
    }

    @Override
    protected List<EnderChestBlockEntity> getTitleTargets() {
        return List.of(this.chest);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, this.enderChest));
    }
}
