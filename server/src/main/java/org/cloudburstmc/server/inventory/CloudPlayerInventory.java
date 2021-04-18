package org.cloudburstmc.server.inventory;

import com.google.common.collect.ImmutableList;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.entity.EntityArmorChangeEvent;
import org.cloudburstmc.api.event.entity.EntityInventoryChangeEvent;
import org.cloudburstmc.api.event.player.PlayerItemHeldEvent;
import org.cloudburstmc.api.inventory.PlayerInventory;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.EntityHuman;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.cloudburstmc.server.registry.CloudItemRegistry.AIR;

/**
 * author: MagicDroidX
 * Nukkit Project
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
                this.sendContents(this.getViewers());
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
        return slot >= 0 && slot <= this.getHotbarSize();
    }

    private PlayerCursorInventory getCursor() {
        return getHolder().getCursorInventory();
    }

    @Override
    public boolean setHotbarSlot(int slot, ItemStack item) {
        if (!isHotbarSlot(slot)) return false;
        return super.setItem(slot, item, true);
    }

    @Override
    public CloudItemStack getHotbarSlot(int slot) {
        if (!isHotbarSlot(slot)) return AIR;
        return super.getItem(slot);
    }

    @Override
    public List<CloudItemStack> getHotbar() {
        ImmutableList.Builder<CloudItemStack> builder = ImmutableList.builder();
        for (int i = 0; i < this.getHotbarSize(); i++) {
            builder.add(getItem(i));
        }
        return builder.build();
    }

    @Nonnull
    @Override
    public ItemStack getCursorItem() {
        return getItem(0);
    }

    @Override
    public boolean setCursorItem(@Nonnull ItemStack item) {
        return super.setItem(0, item);
    }

    @Override
    public void clearCursor() {
        super.setItem(0, AIR, true);
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
        EntityHuman holder = this.getHolder();
        if (holder != null && !((Player) holder).isSpawned()) {
            return;
        }

        if (index == this.getOffHandIndex()) {
            this.sendOffHandSlot(this.getViewers());
            this.sendOffHandSlot(this.getHolder().getViewers());
        } else if (index >= this.getSize()) {
            this.sendArmorSlot(index, this.getViewers());
            this.sendArmorSlot(index, this.getHolder().getViewers());
        } else {
            super.onSlotChange(index, before, send);
        }
    }

    public int getHotbarSize() {
        return 9;
    }

    @Override
    public boolean setItem(int index, ItemStack item) {
        return setItem(index, item, true, false);
    }

    private boolean setItem(int index, ItemStack item, boolean send, boolean ignoreArmorEvents) {
        if (index < 0 || index >= this.size) {
            return false;
        } else if (item.getType() == BlockTypes.AIR || item.getAmount() <= 0) {
            return this.clear(index);
        }

        //Armor change
        if (!ignoreArmorEvents && index >= this.getSize()) {
            EntityArmorChangeEvent ev = new EntityArmorChangeEvent(this.getHolder(), this.getItem(index), item, index);
            CloudServer.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled() && this.getHolder() != null) {
                if (index == this.getOffHandIndex()) {
                    this.sendOffHandSlot(this.getViewers());
                } else {
                    this.sendArmorSlot(index, this.getViewers());
                }
                return false;
            }
            item = ev.getNewItem();
        } else {
            EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent(this.getHolder(), this.getItem(index), item, index);
            CloudServer.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                this.sendSlot(index, this.getViewers());
                return false;
            }
            item = ev.getNewItem();
        }
        ItemStack old = this.getItem(index);
        this.slots.put(index, item);
        this.onSlotChange(index, old, send);
        return true;
    }

    @Override
    public boolean clear(int index, boolean send) {
        if (this.slots.containsKey(index)) {
            ItemStack item = AIR;
            ItemStack old = this.slots.get(index);
            if (index >= this.getSize() && index < this.size) {
                EntityArmorChangeEvent ev = new EntityArmorChangeEvent(this.getHolder(), old, item, index);
                CloudServer.getInstance().getEventManager().fire(ev);
                if (ev.isCancelled()) {
                    if (index >= this.size) {
                        this.sendArmorSlot(index, this.getViewers());
                    } else {
                        this.sendSlot(index, this.getViewers());
                    }
                    return false;
                }
                item = ev.getNewItem();
            } else {
                EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent(this.getHolder(), old, item, index);
                CloudServer.getInstance().getEventManager().fire(ev);
                if (ev.isCancelled()) {
                    if (index >= this.size) {
                        this.sendArmorSlot(index, this.getViewers());
                    } else {
                        this.sendSlot(index, this.getViewers());
                    }
                    return false;
                }
                item = ev.getNewItem();
            }

            if (!item.isNull()) {
                this.slots.put(index, item);
            } else {
                this.slots.remove(index);
            }

            this.onSlotChange(index, old, send);
        }

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
    public void sendContents(Player[] players) {
        List<ItemData> itemData = new ArrayList<>();
        for (int i = 0; i < this.getSize(); ++i) {
            itemData.add(i, ((CloudItemStack) this.getItem(i)).getNetworkData());
        }

        for (CloudPlayer player : (CloudPlayer[]) players) {
            int id = player.getWindowId(this);
            if (id == -1) {
                if (this.getHolder() != player) this.close(player);
                continue;
            }

            InventoryContentPacket pk = new InventoryContentPacket();
            pk.setContents(itemData);
            pk.setContainerId(id);

            player.sendPacket(pk);
        }
    }

    @Override
    public void sendSlot(int index, Player... players) {
        ItemData itemData = this.getItem(index).getNetworkData();

        for (CloudPlayer player : (CloudPlayer[]) players) {
            InventorySlotPacket packet = new InventorySlotPacket();
            packet.setSlot(index);
            packet.setItem(itemData);

            if (player.equals(this.getHolder())) {
                packet.setContainerId(ContainerId.INVENTORY);
                player.sendPacket(packet);
            } else {
                int id = player.getWindowId(this);
                if (id == -1) {
                    this.close(player);
                    continue;
                }
                packet.setContainerId(id);
                player.sendPacket(packet);
            }
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
        super.onOpen(who);
        ContainerOpenPacket pk = new ContainerOpenPacket();
        pk.setId(who.getWindowId(this));
        pk.setType(ContainerType.INVENTORY);
        pk.setBlockPosition(who.getPosition().toInt());
        pk.setUniqueEntityId(who.getUniqueId());
        ((CloudPlayer) who).sendPacket(pk);
    }

    @Override
    public void onClose(Player who) {
        ContainerClosePacket pk = new ContainerClosePacket();
        pk.setId(who.getWindowId(this));
        ((CloudPlayer) who).sendPacket(pk);
        // Player can neer stop viewing their own inventory
        if (who != holder)
            super.onClose(who);
    }

    @Override
    public CloudCraftingGrid getCraftingGrid() {
        return this.getHolder().getCraftingInventory();
    }
}
