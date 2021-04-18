package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import com.nukkitx.protocol.bedrock.packet.MobArmorEquipmentPacket;
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.api.inventory.CreatureInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class CloudCreatureInventory extends BaseInventory implements CreatureInventory {

    private int heldItemIndex = 0;
    private int offHandIndex = 40;

    public CloudCreatureInventory(EntityCreature entity) {
        super(entity, InventoryType.PLAYER);
    }

    @Override
    public EntityCreature getHolder() {
        return (EntityCreature) super.getHolder();
    }

    @Override
    public int getSize() {
        return super.getSize() - 5;
    }

    @Override
    public void setSize(int size) {
        super.setSize(size + 5);
        this.offHandIndex = size + 4;
    }

    protected int getOffHandIndex() {
        return this.offHandIndex;
    }

    @Override
    public int getHeldItemIndex() {
        return this.heldItemIndex;
    }

    public void setHeldItemIndex(int index) {
        if (index >= 0 && index <= this.getSize()) {
            this.heldItemIndex = index;
            this.sendHeldItem(this.getHolder().getViewers());
        }
    }

    @Override
    public void decrementHandCount() {
        this.decrementCount(getHeldItemIndex());
    }

    @Override
    public void incrementHandCount() {
        this.incrementCount(getHeldItemIndex());
    }

    @Override
    public CloudItemStack getItemInHand() {
        return this.getItem(this.getHeldItemIndex());
    }

    @Override
    public boolean setItemInHand(ItemStack item) {
        return this.setItem(this.getHeldItemIndex(), item);
    }

    public void sendHeldItem(Collection<CloudPlayer> players) {
        this.sendHeldItem(players.toArray(new CloudPlayer[0]));
    }

    public void sendHeldItem(CloudPlayer... players) {
        CloudItemStack item = this.getItemInHand();

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setItem(item.getNetworkData());
        packet.setInventorySlot(this.getHeldItemIndex());
        packet.setHotbarSlot(this.getHeldItemIndex());

        for (CloudPlayer player : players) {
            packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
            if (player.equals(this.getHolder())) {
                packet.setRuntimeEntityId(player.getRuntimeId());
                this.sendSlot(this.getHeldItemIndex(), player);
            }

            player.sendPacket(packet);
        }
    }

    @Override
    public CloudItemStack getArmorItem(int index) {
        return this.getItem(this.getSize() + index);
    }

    @Override
    public boolean setArmorItem(int index, ItemStack item, boolean ignoreArmorEvents) {
        return this.setItem(this.getSize() + index, item, ignoreArmorEvents);
    }

    @Override
    public ItemStack[] getArmorContents() {
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            armor[i] = this.getItem(this.getSize() + i);
        }

        return armor;
    }

    @Override
    public ItemStack getOffHand() {
        return this.getItem(offHandIndex);
    }

    @Override
    public void setOffHandContents(ItemStack offhand) {
        if (offhand == null) {
            offhand = CloudItemRegistry.AIR;
        }
        this.setItem(offHandIndex, offhand);
    }

    public void sendOffHandContents(Collection<CloudPlayer> players) {
        this.sendOffHandContents(players.toArray(new CloudPlayer[0]));
    }

    public void sendOffHandContents(CloudPlayer player) {
        this.sendOffHandContents(new CloudPlayer[]{player});
    }

    public void sendOffHandContents(CloudPlayer[] players) {
        ItemStack offHand = this.getOffHand();

        if (offHand == null) {
            offHand = CloudItemRegistry.AIR;
        }

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setItem(((CloudItemStack) offHand).getNetworkData());
        packet.setContainerId(ContainerId.OFFHAND);
        packet.setInventorySlot(1);

        for (CloudPlayer player : players) {
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

    public void sendOffHandSlot(CloudPlayer player) {
        this.sendOffHandSlot(new CloudPlayer[]{player});
    }

    public void sendOffHandSlot(Collection<CloudPlayer> players) {
        this.sendOffHandSlot(players.toArray(new CloudPlayer[0]));
    }

    public void sendOffHandSlot(CloudPlayer[] players) {
        ItemStack offhand = this.getOffHand();

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setItem(((CloudItemStack) offhand).getNetworkData());
        packet.setContainerId(ContainerId.OFFHAND);
        packet.setInventorySlot(1);

        for (CloudPlayer player : players) {
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

    public void sendArmorContents(CloudPlayer player) {
        this.sendArmorContents(new CloudPlayer[]{player});
    }

    public void sendArmorContents(CloudPlayer[] players) {
        ItemStack[] armor = this.getArmorContents();

        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setHelmet(((CloudItemStack) armor[0]).getNetworkData());
        packet.setChestplate(((CloudItemStack) armor[1]).getNetworkData());
        packet.setLeggings(((CloudItemStack) armor[2]).getNetworkData());
        packet.setBoots(((CloudItemStack) armor[3]).getNetworkData());

        for (CloudPlayer player : players) {
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
                items[i] = CloudItemRegistry.AIR;
            }

            if (items[i].isNull()) {
                this.clear(this.getSize() + i);
            } else {
                this.setItem(this.getSize() + i, items[i]);
            }
        }
    }

    public void sendArmorContents(Collection<CloudPlayer> players) {
        this.sendArmorContents(players.toArray(new CloudPlayer[0]));
    }

    public void sendArmorSlot(int index, CloudPlayer player) {
        this.sendArmorSlot(index, new CloudPlayer[]{player});
    }

    public void sendArmorSlot(int index, CloudPlayer[] players) {
        CloudItemStack[] armor = (CloudItemStack[]) this.getArmorContents();

        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setHelmet(armor[0].getNetworkData());
        packet.setChestplate(armor[1].getNetworkData());
        packet.setLeggings(armor[2].getNetworkData());
        packet.setBoots(armor[3].getNetworkData());

        for (CloudPlayer player : players) {
            if (player.equals(this.getHolder())) {
                InventorySlotPacket packet2 = new InventorySlotPacket();
                packet2.setContainerId(ContainerId.ARMOR);
                packet2.setSlot(index - this.getSize());
                packet2.setItem(this.getItem(index).getNetworkData());
                player.sendPacket(packet2);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void sendArmorSlot(int index, Collection<CloudPlayer> players) {
        this.sendArmorSlot(index, players.toArray(new CloudPlayer[0]));
    }


}
