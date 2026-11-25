package org.cloudburstmc.server.container.screen;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.ContainerTypeRegistry;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.List;

/**
 * Opens a storage window at its backing block's position and resolves the block's custom title.
 */
@Log4j2
public abstract class CloudBlockContainerScreen extends CloudContainerScreen {

    @Getter
    protected final Block block;
    private List<? extends BaseBlockEntity> titleTargets = List.of();

    protected CloudBlockContainerScreen(ScreenType<?> type, CloudPlayer player, Block block) {
        super(type, player);
        this.block = block;
    }

    protected static <T extends ContainerBlockEntity> T getOrCreateBlockEntity(Block block, BlockEntityType<?> type, Class<T> implementation) {
        BlockEntity existing = block.getLevel().getBlockEntity(block.getPosition());
        if (existing == null) {
            log.warn("No block entity found at {} for block {}. Creating {}", block.getPosition(), block.getState().getType().getId(), type.getId());
            existing = CloudBlockEntityRegistry.get().newEntity(type, block);
        }

        if (!implementation.isInstance(existing)) {
            throw new IllegalStateException("Block entity at " + block.getPosition() + " is not a " + implementation.getSimpleName() + ": " + existing.getClass().getSimpleName());
        }

        return implementation.cast(existing);
    }

    @Override
    public void open() {
        ContainerType containerType = ContainerTypeRegistry.get(this.getType());
        Container storage = this.getStorageContainer();
        this.sendTitleOverride();

        ContainerOpenPacket packet = new ContainerOpenPacket();
        packet.setType(containerType);
        packet.setBlockPosition(this.block.getPosition());
        this.openWindow(packet, storage);
        this.player.getInventoryManager().sendAllInventories();
    }

    @Override
    public void close() {
        for (BaseBlockEntity target : this.titleTargets) {
            if (!target.isClosed() && target.getLevel() == this.player.getLevel()) {
                this.sendTitleTag(target, null);
            }
        }
        this.titleTargets = List.of();
    }

    protected abstract Container getStorageContainer();

    protected boolean isWithinReach() {
        return this.player.getLevel() == this.block.getLevel() && this.player.canInteractWithBlock(this.block.getPosition());
    }

    @Override
    @Nullable
    public Component getTitle() {
        String override = this.getTitleOverride();
        if (override != null) {
            return BedrockLegacyTextSerializer.getInstance().deserialize(override);
        }

        for (BaseBlockEntity target : this.getTitleTargets()) {
            String customName = target.getCustomName();
            if (customName != null) {
                return BedrockLegacyTextSerializer.getInstance().deserialize(customName);
            }
        }

        BlockEntity entity = this.block.getLevel().getBlockEntity(this.block.getPosition());
        String customName = entity instanceof BaseBlockEntity base ? base.getCustomName() : null;
        return customName == null ? null : BedrockLegacyTextSerializer.getInstance().deserialize(customName);
    }

    protected List<? extends BaseBlockEntity> getTitleTargets() {
        return List.of();
    }

    private void sendTitleOverride() {
        String title = this.getTitleOverride();
        if (title == null) {
            return;
        }

        this.titleTargets = this.getTitleTargets();
        for (BaseBlockEntity target : this.titleTargets) {
            this.sendTitleTag(target, title);
        }
    }

    private void sendTitleTag(BaseBlockEntity target, @Nullable String title) {
        NbtMap original = target.getChunkTag();
        NbtMapBuilder tag = original.toBuilder();
        tag.putString("CustomName", title != null ? title : original.getString("CustomName", ""));

        BlockEntityDataPacket packet = new BlockEntityDataPacket();
        packet.setBlockPosition(target.getPosition());
        packet.setData(tag.build());
        this.player.sendPacket(packet);
    }
}
