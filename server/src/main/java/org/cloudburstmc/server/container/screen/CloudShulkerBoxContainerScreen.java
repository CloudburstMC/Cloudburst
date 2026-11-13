package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.ShulkerBoxBlockEntity;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Shulker-box screen that owns the viewer lifecycle driving its lid animation.
 */
public class CloudShulkerBoxContainerScreen extends CloudChestContainerScreen {

    private boolean lifecycleStarted;

    private CloudShulkerBoxContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.SHULKER_BOX, player, block, ContainerSlotType.SHULKER_BOX, BlockEntityTypes.SHULKER_BOX, SlotGroupTypes.SHULKER_BOX);
    }

    public static @Nullable CloudShulkerBoxContainerScreen create(CloudPlayer player, Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        if (!(blockEntity instanceof ShulkerBoxBlockEntity shulkerBox) || shulkerBox.isOpeningBlocked()) {
            return null;
        }

        return new CloudShulkerBoxContainerScreen(player, block);
    }

    @Override
    public void open() {
        ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) this.chestEntity;
        if (box.isClosed() || box.isOpeningBlocked() || this.block.getLevel().getBlockEntity(this.block.getPosition()) != box) {
            throw new IllegalStateException("Shulker box is no longer available");
        }

        super.open();

        if (!this.player.isSpectator()) {
            box.startOpen();
            this.lifecycleStarted = true;
        }
    }

    @Override
    public void close() {
        if (this.lifecycleStarted) {
            ((ShulkerBoxBlockEntity) this.chestEntity).stopOpen();
            this.lifecycleStarted = false;
        }

        super.close();
    }
}
