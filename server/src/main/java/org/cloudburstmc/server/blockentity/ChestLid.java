package org.cloudburstmc.server.blockentity;

import lombok.Getter;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.block.BlockRedstoneEvent;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;
import org.cloudburstmc.server.block.component.ChestBlockHandlers;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.VanillaChestBlocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestLid {

    private final BaseBlockEntity chest;
    private final Map<CloudPlayer, Boolean> viewers = new HashMap<>();
    private int openers;
    private boolean heldOpen;
    @Getter
    private boolean open;

    public ChestLid(BaseBlockEntity chest) {
        this.chest = chest;
    }

    public boolean hasViewers() {
        return !this.viewers.isEmpty();
    }

    public int signal() {
        return Math.min(15, this.openers + (this.heldOpen ? 1 : 0));
    }

    public void startOpen(CloudPlayer player) {
        boolean counts = !player.isSpectator();
        if (this.viewers.putIfAbsent(player, counts) == null && counts) {
            int previous = this.signal();
            this.openers++;
            this.updateSignal(previous);
            if (this.viewers.containsKey(player)) {
                this.updateLid(true);
            }
        }
    }

    public void stopOpen(CloudPlayer player) {
        if (Boolean.TRUE.equals(this.viewers.remove(player))) {
            int previous = this.signal();
            this.openers--;
            this.updateSignal(previous);
            this.updateLid(this.openers > 0 || this.heldOpen);
        }
    }

    public void setOpen(boolean open) {
        int previous = this.signal();
        this.heldOpen = open;
        this.updateSignal(previous);
        this.updateLid(open);
    }

    public void closeViewers() {
        for (CloudPlayer player : List.copyOf(this.viewers.keySet())) {
            player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
        }
    }

    private void updateLid(boolean open) {
        if (this.open == open || this.chest.isClosed() || this.chest.getLevel() == null) {
            return;
        }

        this.open = open;
        BlockEventPacket packet = new BlockEventPacket();
        packet.setBlockPosition(this.chest.getPosition());
        packet.setEventType(1);
        packet.setEventData(open ? 1 : 0);
        this.chest.getLevel().addChunkPacket(this.chest.getPosition(), packet);
        ChestBlockHandlers.playLidSound(this.chest, open);
    }

    private void updateSignal(int previous) {
        int current = this.signal();
        if (previous == current || this.chest.getLevel() == null || !VanillaChestBlocks.emitsSignal(this.chest.getBlockState().getType())) {
            return;
        }

        Block block = this.chest.getBlock();
        this.chest.getServer().getEventManager().fire(new BlockRedstoneEvent(block, previous, current));
        this.chest.getLevel().updateAroundRedstone(block.getPosition(), null);
        this.chest.getLevel().updateAroundRedstone(Direction.DOWN.relative(block.getPosition()), Direction.UP);
    }
}
