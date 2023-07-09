package org.cloudburstmc.server.inventory;

import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.entity.EntityArmorChangeEvent;
import org.cloudburstmc.api.event.entity.EntityInventoryChangeEvent;
import org.cloudburstmc.api.event.player.PlayerItemHeldEvent;
import org.cloudburstmc.api.inventory.InventoryListener;
import org.cloudburstmc.api.inventory.PlayerInventory;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * author: MagicDroidX
 *
 */
@Log4j2
public class CloudPlayerInventory extends CloudCreatureInventory implements PlayerInventory {

    public static final int SURVIVAL_SLOTS = 36;

    public CloudPlayerInventory(CloudPlayer player) {
        super(player);
    }

    /**
     * Called when a client equips a hotbar inventorySlot. This method should not be used by plugins.
     * This method will call PlayerItemHeldEvent.
     *
     * @param slot hotbar slot Number of the hotbar slot to equip.
     * @return boolean if the equipment change was successful, false if not.
     */
    public boolean equipItem(int slot) {
        if (!isHotbarSlot(slot)) {
            this.sendContents(this.getHolder());
            return false;
        }

        if (this.getHolder() != null) {
            CloudPlayer player = this.getHolder();
            PlayerItemHeldEvent ev = new PlayerItemHeldEvent(player, this.getItem(slot), slot);
            this.getHolder().getLevel().getServer().getEventManager().fire(ev);

            if (ev.isCancelled()) {
                this.sendContents(this.getListeners());
                return false;
            }

            if (player.fishing != null) {
                if (!(this.getItem(slot).equals(player.fishing.getRod()))) {
                    player.stopFishing(false);
                }
            }
        }

        this.setHeldItemIndex(slot, false);
        return true;
    }

    private boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < this.getHotbarSize();
    }

    private PlayerCursorInventory getCursor() {
        return null;
    }

    @Override
    public boolean setHotbarSlot(int slot, ItemStack item) {
        if (!isHotbarSlot(slot)) return false;
        return super.setItem(slot, item, true);
    }

    @Override
    public ItemStack getHotbarSlot(int slot) {
        if (!isHotbarSlot(slot)) return ItemStack.EMPTY;
        return super.getItem(slot);
    }

    @Override
    public List<ItemStack> getHotbar() {
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        for (int i = 0; i < this.getHotbarSize(); i++) {
            builder.add(getItem(i));
        }
        return builder.build();
    }

    @NonNull
    @Override
    public ItemStack getCursorItem() {
        return getCursor().getItem(0);
    }

    @Override
    public boolean setCursorItem(@NonNull ItemStack item) {
        return getCursor().setItem(0, item);
    }

    @Override
    public void clearCursor() {
        getCursor().setItem(0, ItemStack.EMPTY, true);
    }

    @Override
    public void setHeldItemIndex(int index) {
        setHeldItemIndex(index, true);
    }

    @Override
    public void setHeldItemIndex(int index, boolean send) {
        if (index >= 0 && index < this.getHotbarSize()) {
            super.setHeldItemIndex(index);
            if (send) {
                this.sendHeldItem(this.getHolder());
            }
        }
    }

    public void setHeldItemSlot(int slot) {
        if (!isHotbarSlot(slot)) {
            return;
        }
        super.setHeldItemIndex(slot);
        this.sendHeldItem(this.getHolder());
    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        if (!this.getHolder().isSpawned()) {
            return;
        }
        super.onSlotChange(index, before, send);
    }

    public int getHotbarSize() {
        return 9;
    }

    @Override
    public boolean setItem(int index, ItemStack item) {
        return setItem(index, item, true, false);
    }

    @Override
    public boolean setItem(int index, ItemStack item, boolean send) {
        return setItem(index, item, send, false);
    }

    private boolean setItem(int index, ItemStack item, boolean send, boolean ignoreArmorEvents) {
        if (index < 0 || index >= this.slots.length) {
            return false;
        } else if (item.getType() == BlockTypes.AIR || item.getCount() <= 0) {
            return this.clear(index, send);
        }

        //Armor change --  TODO remove this from here, setArmorItem should be used for changing armor slots
        if (!ignoreArmorEvents && index >= this.getSize()) {
            EntityArmorChangeEvent ev = new EntityArmorChangeEvent(this.getHolder(), this.getItem(index), item, index);
            CloudServer.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled() && this.getHolder() != null) {
                this.sendArmorSlot(index, this.getListeners());
                return false;
            }
            item = ev.getNewItem();
        } else {
            EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent(this.getHolder(), this.getItem(index), item, index);
            CloudServer.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                this.sendSlot(index, this.getListeners());
                return false;
            }
            item = ev.getNewItem();
        }
        ItemStack old = this.getItem(index);
        this.slots[index] = item;
        this.onSlotChange(index, old, send);
        return true;
    }

    @Override
    public boolean clear(int index, boolean send) {
        this.checkSlotIndex(index);
        ItemStack item = ItemStack.EMPTY;
        ItemStack old = this.slots[index];
        if (index >= this.getSize() && index < this.getSize()) {
            EntityArmorChangeEvent ev = new EntityArmorChangeEvent(this.getHolder(), old, item, index);
            CloudServer.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                if (index >= this.getSize()) {
                    this.sendArmorSlot(index, this.getListeners());
                } else {
                    this.sendSlot(index, this.getListeners());
                }
                return false;
            }
            item = ev.getNewItem();
        } else {
            EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent(this.getHolder(), old, item, index);
            CloudServer.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                if (index >= this.getSize()) {
                    this.sendArmorSlot(index, this.getListeners());
                } else {
                    this.sendSlot(index, this.getListeners());
                }
                return false;
            }
            item = ev.getNewItem();
        }

        this.slots[index] = item;

        this.onSlotChange(index, old, send);

        return true;
    }

    @Override
    public void clearAll() {
        int limit = this.getSize() + 5;
        for (int index = 0; index < limit; ++index) {
            this.clear(index);
        }
    }

    @Override
    public void sendContents(InventoryListener[] listeners) {
        List<ItemData> itemData = new ArrayList<>();
        for (int i = 0; i < this.getSize(); ++i) {
            itemData.add(i, ItemUtils.toNetwork(this.getItem(i)));
        }

        for (InventoryListener listener : listeners) {
            listener.onInventoryContentsChange(this);
        }
    }

    @Override
    public void sendSlot(int index, InventoryListener... listeners) {
        ItemData itemData = ItemUtils.toNetwork(this.getItem(index));

        for (InventoryListener listener : listeners) {
            listener.onInventorySlotChange(this, index, this.getItem(index));
        }
    }

    @Override
    public void sendCreativeContents() {
        CloudPlayer p = this.getHolder();

        CreativeContentPacket pk;

        if (!p.isSpectator()) {
            pk = CloudItemRegistry.get().getCreativeContent();
        } else {
            pk = new CreativeContentPacket();
            pk.setContents(new ItemData[0]);
        }

        p.sendPacket(pk);
    }

    @Override
    public CloudPlayer getHolder() {
        return (CloudPlayer) super.getHolder();
    }

    @Override
    public void onOpen(Player who) {
    }

    @Override
    public void onClose(Player who) {
    }

    @Override
    public @NonNull CloudCraftingGrid getCraftingGrid() {
        return null; // TODO
    }
}
