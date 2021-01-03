package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerUIInventory extends BaseInventory {
    private final Player player;

    private final PlayerCursorInventory cursorInventory;
    private final CraftingGrid craftingGrid;
    private final BigCraftingGrid bigCraftingGrid;

    public PlayerUIInventory(Player player) {
        super(player, InventoryType.UI, new HashMap<>(), 51);
        this.player = player;

        this.cursorInventory = new PlayerCursorInventory(this);
        this.craftingGrid = new CraftingGrid(this);
        this.bigCraftingGrid = new BigCraftingGrid(this);
    }

    public PlayerCursorInventory getCursorInventory() {
        return cursorInventory;
    }

    public CraftingGrid getCraftingGrid() {
        return craftingGrid;
    }

    public BigCraftingGrid getBigCraftingGrid() {
        return bigCraftingGrid;
    }

    @Override
    public void setSize(int size) {
        throw new UnsupportedOperationException("UI size is immutable");
    }

    @Override
    public void sendSlot(int index, Player... target) {
        InventorySlotPacket packet = new InventorySlotPacket();
        packet.setSlot(index);
        packet.setItem(((CloudItemStack) this.getItem(index)).getNetworkData());

        for (Player p : target) {
            if (p == this.getHolder()) {
                packet.setContainerId(ContainerId.UI);
                p.sendPacket(packet);
            } else {
                int id;

                if ((id = p.getWindowId(this)) == ContainerId.NONE) {
                    this.close(p);
                    continue;
                }
                packet.setContainerId(id);
                p.sendPacket(packet);
            }
        }
    }

    @Override
    public void sendContents(Player... target) {
        InventoryContentPacket packet = new InventoryContentPacket();
        List<ItemData> contents = new ArrayList<>();
        for (int i = 0; i < this.getSize(); ++i) {
            contents.add(i, ((CloudItemStack) this.getItem(i)).getNetworkData());
        }
        packet.setContents(contents);

        for (Player p : target) {
            if (p == this.getHolder()) {
                packet.setContainerId(ContainerId.UI);
                p.sendPacket(packet);
            } else {
                int id;

                if ((id = p.getWindowId(this)) == ContainerId.NONE) {
                    this.close(p);
                    continue;
                }
                packet.setContainerId(id);
                p.sendPacket(packet);
            }
        }
    }

    @Override
    public int getSize() {
        return 51;
    }

    @Override
    public Player getHolder() {
        return player;
    }
}
