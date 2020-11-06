package org.cloudburstmc.server.inventory;

import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class PlayerUIComponent extends BaseInventory {
    private final PlayerUIInventory playerUI;
    private final int offset;
    private final int size;

    PlayerUIComponent(PlayerUIInventory playerUI, int offset, int size) {
        super(playerUI.holder, InventoryType.UI, Collections.emptyMap(), size);
        this.playerUI = playerUI;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setMaxStackSize(int size) {
        throw new UnsupportedOperationException();
    }


    @Override
    public String getTitle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemStack getItem(int index) {
        return this.playerUI.getItem(index + this.offset);
    }

    @Override
    public boolean setItem(int index, ItemStack item, boolean send) {
        return this.playerUI.setItem(index + this.offset, item, send);
    }

    @Override
    public Map<Integer, ItemStack> getContents() {
        Map<Integer, ItemStack> contents = playerUI.getContents();
        contents.keySet().removeIf(slot -> slot < offset || slot > offset + size);
        return contents;
    }


    @Override
    public void sendContents(Player... players) {
        this.playerUI.sendContents(players);
    }

    @Override
    public void sendSlot(int index, Player... players) {
        playerUI.sendSlot(index + this.offset, players);
    }

    @Override
    public Set<Player> getViewers() {
        return playerUI.viewers;
    }

    @Override
    public InventoryType getType() {
        return playerUI.type;
    }

    @Override
    public void onOpen(Player who) {

    }

    @Override
    public boolean open(Player who) {
        return false;
    }

    @Override
    public void close(Player who) {

    }

    @Override
    public void onClose(Player who) {

    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        this.playerUI.onSlotChange(index + this.offset, before, send);
    }
}
