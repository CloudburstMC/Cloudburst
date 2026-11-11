package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.inventory.LecternScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockLecternView;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.blockentity.LecternBlockEntity;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudLecternContainerScreen extends CloudInventoryScreen implements LecternScreen {

    private final LecternBlockEntity lectern;
    private boolean closedByReader;

    public CloudLecternContainerScreen(CloudPlayer player, LecternBlockEntity lectern) {
        super(ScreenTypes.LECTERN, player);
        this.lectern = lectern;
    }

    @Override
    public Block getBlock() {
        return this.lectern.getBlock();
    }

    @Override
    public BlockLecternView getLectern() {
        return this.lectern;
    }

    public LecternBlockEntity lecternEntity() {
        return this.lectern;
    }

    public boolean isAvailable() {
        Block block = this.getBlock();
        return !this.lectern.isClosed() && this.lectern.hasBook()
                && this.player.getLevel() == block.getLevel()
                && this.player.canInteractWithBlock(block.getPosition())
                && block.getLevel().getBlock(block.getPosition()).getState().getType() == BlockTypes.LECTERN
                && block.getLevel().getBlockEntity(block.getPosition()) == this.lectern;
    }

    @Override
    public void open() {
        if (!this.isAvailable()) {
            throw new IllegalStateException("Lectern is no longer available");
        }

        this.closedByReader = false;
        this.lectern.spawnTo(this.player);
        ContainerOpenPacket packet = new ContainerOpenPacket();
        packet.setType(ContainerType.LECTERN);
        packet.setBlockPosition(this.lectern.getPosition());
        packet.setUniqueEntityId(-1);
        this.openWindow(packet);
        this.player.getInventoryManager().sendAllInventories();
    }

    public void closeByReader() {
        this.closedByReader = true;
        this.player.getInventoryManager().closeScreen(InventoryCloseEvent.Reason.PLAYER);
    }

    @Override
    public void close() {
        if (this.closedByReader) {
            return;
        }

        BlockEntityDataPacket packet = new BlockEntityDataPacket();
        Vector3i position = this.lectern.getPosition();
        packet.setBlockPosition(position);
        packet.setData(NbtMap.builder().putString("id", "Lectern")
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ())
                .build());
        this.player.sendPacket(packet);

        if (this.lectern.hasBook()) {
            this.player.getServer().getGlobalScheduler().runDelayed(null, _ -> {
                if (this.player.isConnected() && this.isAvailable()) {
                    this.lectern.spawnTo(this.player);
                }
            }, 2);
        }
    }

    @Override
    protected void setupMappings() {
        this.addMapping(new ContainerMapping(ContainerSlotType.LEVEL_ENTITY, this.lectern, 1, 0));
    }
}
