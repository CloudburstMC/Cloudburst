package org.cloudburstmc.server.container.screen;

import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.ContainerTypeRegistry;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

/**
 * Base class for container screens that are backed by a block in the world
 * (chest, furnace, anvil, etc.).
 *
 * <p>On {@link #open()} it sends a {@code ContainerOpenPacket} to the client
 * using the correct {@link ContainerType} for this screen type and the
 * block's world position.  The window ID is assigned and tracked on
 * {@link CloudPlayer} so {@link #close()} can clean it up.</p>
 */
@Log4j2
public abstract class CloudBlockContainerScreen extends CloudContainerScreen {

    protected final Block block;

    protected CloudBlockContainerScreen(ScreenType<?> type, CloudPlayer player, Block block) {
        super(type, player);
        this.block = block;
    }

    @SuppressWarnings("unchecked")
    protected static <T extends ContainerBlockEntity> T getOrCreateBlockEntity(
            Block block, BlockEntityType<?> type) {
        BlockEntity existing = block.getLevel().getBlockEntity(block.getPosition());
        if (existing == null) {
            log.warn("No block entity found at {} for block {}; auto-creating {}",
                    block.getPosition(), block.getState().getType().getId(),
                    type.getIdentifier());
            return (T) BlockEntityRegistry.get().newEntity((BlockEntityType) type, block);
        }
        if (!(existing instanceof ContainerBlockEntity)) {
            throw new IllegalStateException(
                    "Block entity at " + block.getPosition() + " is not a ContainerBlockEntity: " + existing.getClass().getSimpleName());
        }
        return (T) existing;
    }

    public void open() {
        ContainerType containerType = ContainerTypeRegistry.get(getType());
        Vector3i pos = block.getPosition();

        byte windowId = player.assignContainerId(getStorageContainer());

        ContainerOpenPacket pkt = new ContainerOpenPacket();
        pkt.setId(windowId);
        pkt.setType(containerType);
        pkt.setBlockPosition(pos);
        player.sendPacket(pkt);

        player.getInventoryManager().sendAllInventories();
    }

    protected abstract Container getStorageContainer();

    public Block getBlock() {
        return block;
    }

    @Override
    @Nullable
    public Component getTitle() {
        String override = getTitleOverride();
        if (override != null) {
            return BedrockLegacyTextSerializer.getInstance().deserialize(override);
        }
        BlockEntity be = block.getLevel().getBlockEntity(block.getPosition());
        if (be instanceof BaseBlockEntity base && base.hasCustomName()) {
            return BedrockLegacyTextSerializer.getInstance().deserialize(base.getCustomName());
        }
        return null;
    }
}
