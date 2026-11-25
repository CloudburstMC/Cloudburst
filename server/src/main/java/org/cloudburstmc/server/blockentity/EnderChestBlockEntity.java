package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.EnderChest;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.ChestBlockHandlers;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Set;

public class EnderChestBlockEntity extends BaseBlockEntity implements EnderChest {

    private final ChestLid lid = new ChestLid(this);

    public EnderChestBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public boolean isBlocked() {
        return ChestBlockHandlers.isBlockedByBlock(this.getBlock());
    }

    @Override
    public boolean isLidOpen() {
        return this.lid.isOpen();
    }

    @Override
    public void setLidOpen(boolean open) {
        this.lid.setOpen(open);
    }

    public void startOpen(CloudPlayer player) {
        this.lid.startOpen(player);
    }

    public void stopOpen(CloudPlayer player) {
        this.lid.stopOpen(player);
    }

    @Override
    public void close() {
        if (!this.isClosed()) {
            this.lid.closeViewers();
            super.close();
        }
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("Items", "Findable", "LootTable", "LootTableSeed", "pairx", "pairz", "pairlead");
    }
}
