package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.blockentity.EnderChest;
import org.cloudburstmc.api.inventory.ContainerInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.entity.EntityHuman;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CloudEnderChestInventory extends CloudContainer implements ContainerInventory {

    public CloudEnderChestInventory(EntityHuman player) {
        super(player, InventoryType.ENDER_CHEST);
    }

    @Override
    public CloudPlayer getHolder() {
        return (CloudPlayer) this.holder;
    }

    @Override
    public void onOpen(Player who) {
        if (who != this.getHolder()) {
            return;
        }
        super.onOpen(who);
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId(who.getWindowId(this));
        containerOpenPacket.setType(NetworkUtils.inventoryToNetwork(this.getType()));
        EnderChest chest = who.getViewingEnderChest();
        if (chest != null) {
            containerOpenPacket.setBlockPosition(chest.getPosition());
        } else {
            containerOpenPacket.setBlockPosition(Vector3i.ZERO);
        }

        ((CloudPlayer) who).sendPacket(containerOpenPacket);

        this.sendContents(who);

        if (chest != null && chest.getInventory().getViewers().size() == 1) {
            CloudLevel level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition().add(0.5, 0.5, 0.5), SoundEvent.ENDERCHEST_OPEN);
                CloudContainer.sendBlockEventPacket(level.getBlockEntity(chest.getPosition()), 1);
            }
        }
    }

    @Override
    public void onClose(Player player) {
        CloudPlayer who = (CloudPlayer)player;
        ContainerClosePacket containerClosePacket = new ContainerClosePacket();
        containerClosePacket.setId(who.getWindowId(this));
        who.sendPacket(containerClosePacket);
        super.onClose(who);

        EnderChest chest = who.getViewingEnderChest();
        if (chest != null && chest.getInventory().getViewers().size() == 1) {
            CloudLevel level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition().add(0.5, 0.5, 0.5), SoundEvent.ENDERCHEST_CLOSED);
                CloudContainer.sendBlockEventPacket(level.getBlockEntity(chest.getPosition()), 0);
            }

            who.setViewingEnderChest(null);
        }

        super.onClose(who);
    }

    @Override
    public void saveInventory(NbtMapBuilder tag) {
        List<NbtMap> enderItems = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> slot : this.getContents().entrySet()) {
            ItemStack item = slot.getValue();
            if (!ItemUtils.isNull(item)) {
                enderItems.add(ItemUtils.serializeItem(item, slot.getKey()));
            }
        }
        tag.putList("EnderChestInventory", NbtType.COMPOUND, enderItems);
    }
}
