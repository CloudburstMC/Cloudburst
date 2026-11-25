package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.inventory.ChestScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.block.component.ChestBlockHandlers;
import org.cloudburstmc.server.blockentity.ChestBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class CloudChestScreen extends CloudBlockStorageScreen implements ChestScreen {

    private final ChestBlockEntity chest;
    private final @Nullable ChestBlockEntity pair;
    private final CloudContainer inventory;
    private boolean opened;

    private CloudChestScreen(CloudPlayer player, Block block, ChestBlockEntity chest, @Nullable ChestBlockEntity pair, CloudContainer inventory) {
        super(pair == null ? ScreenTypes.CHEST : ScreenTypes.DOUBLE_CHEST, player, block, ContainerSlotType.LEVEL_ENTITY, chest, pair == null ? SlotGroupTypes.CHEST : SlotGroupTypes.DOUBLE_CHEST);
        this.chest = chest;
        this.pair = pair;
        this.inventory = inventory;
    }

    public static @Nullable CloudChestScreen create(CloudPlayer player, Block block) {
        BlockEntity entity = block.getLevel().getBlockEntity(block.getPosition());
        if (entity == null) {
            ChestBlockHandlers.CHEST_ADDED.execute(block);
            entity = block.getLevel().getBlockEntity(block.getPosition());
        }

        if (!(entity instanceof ChestBlockEntity chest) || chest.isBlocked()) {
            return null;
        }

        CloudContainer inventory = chest.getContainer();
        ChestBlockEntity pair = chest.getPair();
        if (chest.isPaired() && pair == null) {
            return null;
        }

        return new CloudChestScreen(player, block, chest, pair, inventory);
    }

    @Override
    public ChestBlockEntity getChest() {
        return this.chest;
    }

    @Override
    public boolean canOpen() {
        return this.canKeepOpen() && this.chest.getPair() == this.pair && !this.chest.isBlocked() && this.chest.getContainer() == this.inventory;
    }

    @Override
    public boolean canKeepOpen() {
        return !this.chest.isClosed() && (this.pair == null || !this.pair.isClosed()) && this.isWithinReach();
    }

    @Override
    public void open() {
        super.open();
        this.opened = true;
        this.chest.startOpen(this.player);
        if (this.opened && this.pair != null) {
            this.pair.startOpen(this.player);
        }
    }

    @Override
    public void close() {
        if (this.opened) {
            this.opened = false;
            this.chest.stopOpen(this.player);
            if (this.pair != null) {
                this.pair.stopOpen(this.player);
            }
        }

        super.close();
    }

    @Override
    protected CloudContainer getStorageContainer() {
        return this.inventory;
    }

    @Override
    protected List<ChestBlockEntity> getTitleTargets() {
        if (this.pair == null) {
            return List.of(this.chest);
        }
        return this.chest.isPairLead() ? List.of(this.chest, this.pair) : List.of(this.pair, this.chest);
    }
}
