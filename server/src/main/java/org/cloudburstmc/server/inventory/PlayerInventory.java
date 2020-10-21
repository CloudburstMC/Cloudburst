package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.entity.impl.Human;
import org.cloudburstmc.server.event.entity.EntityArmorChangeEvent;
import org.cloudburstmc.server.event.entity.EntityInventoryChangeEvent;
import org.cloudburstmc.server.event.player.PlayerItemHeldEvent;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemStacks;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Log4j2
public class PlayerInventory extends BaseInventory {

    public static final int SURVIVAL_SLOTS = 36;

    protected int itemInHandIndex = 0;
    private int offHandIndex = 40;

    public PlayerInventory(Human player) {
        super(player, InventoryType.PLAYER);
    }

    @Override
    public int getSize() {
        return super.getSize() - 5;
    }

    @Override
    public void setSize(int size) {
        super.setSize(size + 5);
        this.offHandIndex = size + 4;
        this.sendContents(this.getViewers());
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
            this.sendContents((Player) this.getHolder());
            return false;
        }

        if (this.getHolder() instanceof Player) {
            Player player = (Player) this.getHolder();
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

    @Deprecated
    public int getHotbarSlotIndex(int index) {
        return index;
    }

    @Deprecated
    public void setHotbarSlotIndex(int index, int slot) {

    }

    public int getHeldItemIndex() {
        return this.itemInHandIndex;
    }

    public void setHeldItemIndex(int index) {
        setHeldItemIndex(index, true);
    }

    public void setHeldItemIndex(int index, boolean send) {
        if (index >= 0 && index < this.getHotbarSize()) {
            this.itemInHandIndex = index;

            if (this.getHolder() instanceof Player && send) {
                this.sendHeldItem((Player) this.getHolder());
            }

            this.sendHeldItem(this.getHolder().getViewers());
        }
    }

    public void decrementHandCount() {
        this.decrementCount(getHeldItemIndex());
    }

    public void incrementHandCount() {
        this.incrementCount(getHeldItemIndex());
    }

    public ItemStack getItemInHand() {
        ItemStack item = this.getItem(this.getHeldItemIndex());
        if (item != null) {
            return item;
        } else {
            return ItemStacks.AIR;
        }
    }

    public boolean setItemInHand(ItemStack item) {
        return this.setItem(this.getHeldItemIndex(), item);
    }

    public void setHeldItemSlot(int slot) {
        if (!isHotbarSlot(slot)) {
            return;
        }

        this.itemInHandIndex = slot;

        if (this.getHolder() instanceof Player) {
            this.sendHeldItem((Player) this.getHolder());
        }

        this.sendHeldItem(this.getViewers());
    }

    public void sendHeldItem(Player... players) {
        ItemStack item = this.getItemInHand();

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setItem(((CloudItemStack) item).getNetworkData());
        packet.setInventorySlot(this.getHeldItemIndex());
        packet.setHotbarSlot(this.getHeldItemIndex());

        for (Player player : players) {
            packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
            if (player.equals(this.getHolder())) {
                packet.setRuntimeEntityId(player.getRuntimeId());
                this.sendSlot(this.getHeldItemIndex(), player);
            }

            player.sendPacket(packet);
        }
    }

    public void sendHeldItem(Collection<Player> players) {
        this.sendHeldItem(players.toArray(new Player[0]));
    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        Human holder = this.getHolder();
        if (holder instanceof Player && !((Player) holder).spawned) {
            return;
        }

        if (index == this.offHandIndex) {
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

    public ItemStack getArmorItem(int index) {
        return this.getItem(this.getSize() + index);
    }

    public boolean setArmorItem(int index, ItemStack item) {
        return this.setArmorItem(index, item, false);
    }

    public boolean setArmorItem(int index, ItemStack item, boolean ignoreArmorEvents) {
        return this.setItem(this.getSize() + index, item, ignoreArmorEvents);
    }

    public ItemStack getHelmet() {
        return this.getItem(this.getSize());
    }

    public ItemStack getChestplate() {
        return this.getItem(this.getSize() + 1);
    }

    public ItemStack getLeggings() {
        return this.getItem(this.getSize() + 2);
    }

    public ItemStack getBoots() {
        return this.getItem(this.getSize() + 3);
    }

    public boolean setHelmet(ItemStack helmet) {
        return this.setItem(this.getSize(), helmet);
    }

    public boolean setChestplate(ItemStack chestplate) {
        return this.setItem(this.getSize() + 1, chestplate);
    }

    public boolean setLeggings(ItemStack leggings) {
        return this.setItem(this.getSize() + 2, leggings);
    }

    public boolean setBoots(ItemStack boots) {
        return this.setItem(this.getSize() + 3, boots);
    }

    @Override
    public boolean setItem(int index, ItemStack item) {
        return setItem(index, item, true, false);
    }

    private boolean setItem(int index, ItemStack item, boolean send, boolean ignoreArmorEvents) {
        if (index < 0 || index >= this.size) {
            return false;
        } else if (item.getType() == AIR || item.getAmount() <= 0) {
            return this.clear(index);
        }

        //Armor change
        if (!ignoreArmorEvents && index >= this.getSize()) {
            EntityArmorChangeEvent ev = new EntityArmorChangeEvent(this.getHolder(), this.getItem(index), item, index);
            Server.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled() && this.getHolder() != null) {
                if (index == this.offHandIndex) {
                    this.sendOffHandSlot(this.getViewers());
                } else {
                    this.sendArmorSlot(index, this.getViewers());
                }
                return false;
            }
            item = ev.getNewItem();
        } else {
            EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent(this.getHolder(), this.getItem(index), item, index);
            Server.getInstance().getEventManager().fire(ev);
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
            ItemStack item = ItemStacks.AIR;
            ItemStack old = this.slots.get(index);
            if (index >= this.getSize() && index < this.size) {
                EntityArmorChangeEvent ev = new EntityArmorChangeEvent(this.getHolder(), old, item, index);
                Server.getInstance().getEventManager().fire(ev);
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
                Server.getInstance().getEventManager().fire(ev);
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

    public ItemStack[] getArmorContents() {
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            armor[i] = this.getItem(this.getSize() + i);
        }

        return armor;
    }

    @Override
    public void clearAll() {
        int limit = this.getSize() + 5;
        for (int index = 0; index < limit; ++index) {
            this.clear(index);
        }
    }

    public ItemStack getOffHand() {
        return this.getItem(offHandIndex);
    }

    public void setOffHandContents(ItemStack offhand) {
        if (offhand == null) {
            offhand = ItemStacks.AIR;
        }
        this.setItem(offHandIndex, offhand);
    }

    public void sendOffHandContents(Collection<Player> players) {
        this.sendOffHandContents(players.toArray(new Player[0]));
    }

    public void sendOffHandContents(Player player) {
        this.sendOffHandContents(new Player[]{player});
    }

    public void sendOffHandContents(Player[] players) {
        ItemStack offHand = this.getOffHand();

        if (offHand == null) {
            offHand = ItemStacks.AIR;
        }

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setItem(((CloudItemStack) offHand).getNetworkData());
        packet.setContainerId(ContainerId.OFFHAND);
        packet.setInventorySlot(1);

        for (Player player : players) {
            if (player.equals(this.getHolder())) {
                InventoryContentPacket invPacket = new InventoryContentPacket();
                invPacket.setContainerId(ContainerId.OFFHAND);
                invPacket.setContents(ItemUtils.toNetwork(Collections.singleton(offHand)));
                player.sendPacket(invPacket);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void sendOffHandSlot(Player player) {
        this.sendOffHandSlot(new Player[]{player});
    }

    public void sendOffHandSlot(Collection<Player> players) {
        this.sendOffHandSlot(players.toArray(new Player[0]));
    }

    public void sendOffHandSlot(Player[] players) {
        ItemStack offhand = this.getOffHand();

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setItem(((CloudItemStack) offhand).getNetworkData());
        packet.setContainerId(ContainerId.OFFHAND);
        packet.setInventorySlot(1);

        for (Player player : players) {
            if (player.equals(this.getHolder())) {
                InventorySlotPacket slotPacket = new InventorySlotPacket();
                slotPacket.setContainerId(ContainerId.OFFHAND);
                slotPacket.setItem(((CloudItemStack) offhand).getNetworkData());
                slotPacket.setSlot(0); // Not sure why offhand uses slot 0 in SlotPacket and 1 in MobEq Packet
                player.sendPacket(slotPacket);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void sendArmorContents(Player player) {
        this.sendArmorContents(new Player[]{player});
    }

    public void sendArmorContents(Player[] players) {
        ItemStack[] armor = this.getArmorContents();

        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setHelmet(((CloudItemStack) armor[0]).getNetworkData());
        packet.setChestplate(((CloudItemStack) armor[1]).getNetworkData());
        packet.setLeggings(((CloudItemStack) armor[2]).getNetworkData());
        packet.setBoots(((CloudItemStack) armor[3]).getNetworkData());

        for (Player player : players) {
            if (player.equals(this.getHolder())) {
                InventoryContentPacket packet2 = new InventoryContentPacket();
                packet2.setContainerId(ContainerId.ARMOR);
                packet2.setContents(ItemUtils.toNetwork(Arrays.asList(armor)));
                player.sendPacket(packet2);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void setArmorContents(ItemStack[] items) {
        if (items.length < 4) {
            ItemStack[] newItems = new ItemStack[4];
            System.arraycopy(items, 0, newItems, 0, items.length);
            items = newItems;
        }

        for (int i = 0; i < 4; ++i) {
            if (items[i] == null) {
                items[i] = ItemStacks.AIR;
            }

            if (items[i].isNull()) {
                this.clear(this.getSize() + i);
            } else {
                this.setItem(this.getSize() + i, items[i]);
            }
        }
    }

    public void sendArmorContents(Collection<Player> players) {
        this.sendArmorContents(players.toArray(new Player[0]));
    }

    public void sendArmorSlot(int index, Player player) {
        this.sendArmorSlot(index, new Player[]{player});
    }

    public void sendArmorSlot(int index, Player[] players) {
        CloudItemStack[] armor = (CloudItemStack[]) this.getArmorContents();

        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setHelmet(armor[0].getNetworkData());
        packet.setChestplate(armor[1].getNetworkData());
        packet.setLeggings(armor[2].getNetworkData());
        packet.setBoots(armor[3].getNetworkData());

        for (Player player : players) {
            if (player.equals(this.getHolder())) {
                InventorySlotPacket packet2 = new InventorySlotPacket();
                packet2.setContainerId(ContainerId.ARMOR);
                packet2.setSlot(index - this.getSize());
                packet2.setItem(((CloudItemStack) this.getItem(index)).getNetworkData());
                player.sendPacket(packet2);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void sendArmorSlot(int index, Collection<Player> players) {
        this.sendArmorSlot(index, players.toArray(new Player[0]));
    }

    @Override
    public void sendContents(Player player) {
        this.sendContents(new Player[]{player});
    }

    @Override
    public void sendContents(Collection<Player> players) {
        this.sendContents(players.toArray(new Player[0]));
    }

    @Override
    public void sendContents(Player[] players) {
        ItemData[] itemData = new ItemData[this.getSize()];
        for (int i = 0; i < this.getSize(); ++i) {
            itemData[i] = ((CloudItemStack) this.getItem(i)).getNetworkData();
        }

        for (Player player : players) {
            int id = player.getWindowId(this);
            if (id == -1) {
                if (this.getHolder() != player) this.close(player);
                continue;
            }

            InventoryContentPacket pk = new InventoryContentPacket();
            pk.setContents(new ItemData[itemData.length]);
            for (int i = 0; i < this.getSize(); ++i) {
                pk.getContents()[i] = itemData[i];
            }
            pk.setContainerId(id);

            player.sendPacket(pk);
        }
    }

    @Override
    public void sendSlot(int index, Player player) {
        this.sendSlot(index, new Player[]{player});
    }

    @Override
    public void sendSlot(int index, Collection<Player> players) {
        this.sendSlot(index, players.toArray(new Player[0]));
    }

    @Override
    public void sendSlot(int index, Player... players) {
        ItemData itemData = ((CloudItemStack) this.getItem(index)).getNetworkData();

        for (Player player : players) {
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

    public void sendCreativeContents() {
        if (!(this.getHolder() instanceof Player)) {
            return;
        }
        Player p = (Player) this.getHolder();

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
    public Human getHolder() {
        return (Human) super.getHolder();
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);
        ContainerOpenPacket pk = new ContainerOpenPacket();
        pk.setId(who.getWindowId(this));
        pk.setType(ContainerType.INVENTORY);
        pk.setBlockPosition(who.getPosition().toInt());
        pk.setUniqueEntityId(who.getUniqueId());
        who.sendPacket(pk);
    }

    @Override
    public void onClose(Player who) {
        ContainerClosePacket pk = new ContainerClosePacket();
        pk.setId(who.getWindowId(this));
        who.sendPacket(pk);
        // Player can neer stop viewing their own inventory
        if (who != holder)
            super.onClose(who);
    }
}
