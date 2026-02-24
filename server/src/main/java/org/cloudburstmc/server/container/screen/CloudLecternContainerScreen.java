package org.cloudburstmc.server.container.screen;

import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.inventory.LecternScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockLecternView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.blockentity.LecternBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ContainerTypeRegistry;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudLecternView;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

/**
 * Screen implementation for the lectern.
 *
 * <p>The lectern is a book viewer — the player's own inventory is <strong>not</strong> shown
 * alongside it. This screen therefore extends {@link CloudInventoryScreen} directly rather
 * than {@link CloudContainerScreen}, and the only slot group is the single lectern book slot.</p>
 */
@Log4j2
public class CloudLecternContainerScreen extends CloudInventoryScreen implements LecternScreen {

    private final Block block;
    private final CloudLecternView lectern;

    public CloudLecternContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.LECTERN, player);
        this.block = block;
        this.lectern = new CloudLecternView(block, new CloudContainer(1));
    }

    @Nullable
    public static LecternBlockEntity getOrCreateLectern(Block block) {
        BlockEntity existing = block.getLevel().getBlockEntity(block.getPosition());
        if (existing instanceof LecternBlockEntity lecternBE) {
            return lecternBE;
        }
        if (existing != null) {
            log.warn("Unexpected block entity at {} for lectern: {}", block.getPosition(), existing.getClass().getSimpleName());
            return null;
        }
        log.warn("No block entity found at {} for lectern; auto-creating", block.getPosition());
        return (LecternBlockEntity) BlockEntityRegistry.get().newEntity(BlockEntityTypes.LECTERN, block);
    }

    @Override
    public BlockLecternView getLectern() {
        return lectern;
    }

    @Override
    public void open() {
        ContainerType containerType = ContainerTypeRegistry.get(getType());

        byte windowId = player.assignContainerId(lectern.getContainer());

        ContainerOpenPacket pkt = new ContainerOpenPacket();
        pkt.setId(windowId);
        pkt.setType(containerType);
        pkt.setBlockPosition(block.getPosition());
        player.sendPacket(pkt);

        player.getInventoryManager().sendAllInventories();
    }

    @Override
    protected void setupMappings() {
        this.addMapping(new ContainerMapping(ContainerSlotType.LEVEL_ENTITY, lectern, 1, 0));
    }

    public Block getBlock() {
        return block;
    }
}
