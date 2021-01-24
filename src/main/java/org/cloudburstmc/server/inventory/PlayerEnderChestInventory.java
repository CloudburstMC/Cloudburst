package org.cloudburstmc.server.inventory;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.blockentity.EnderChest;
import org.cloudburstmc.server.entity.impl.Human;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.player.Player;

public class PlayerEnderChestInventory extends BaseInventory {

    public PlayerEnderChestInventory(Human player) {
        super(player, InventoryType.ENDER_CHEST);
    }

    @Override
    public Human getHolder() {
        return (Human) this.holder;
    }

    @Override
    public void onOpen(Player who) {
        if (who != this.getHolder()) {
            return;
        }
        super.onOpen(who);
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId(who.getWindowId(this));
        containerOpenPacket.setType(ContainerType.from(this.getType().getNetworkType()));
        EnderChest chest = who.getViewingEnderChest();
        if (chest != null) {
            containerOpenPacket.setBlockPosition(chest.getPosition());
        } else {
            containerOpenPacket.setBlockPosition(Vector3i.ZERO);
        }

        who.sendPacket(containerOpenPacket);

        this.sendContents(who);

        if (chest != null && chest.getInventory().getViewers().size() == 1) {
            World world = this.getHolder().getWorld();
            if (world != null) {
                world.addLevelSoundEvent(this.getHolder().getPosition().add(0.5, 0.5, 0.5), SoundEvent.ENDERCHEST_OPEN);
                ContainerInventory.sendBlockEventPacket(world.getBlockEntity(chest.getPosition()), 1);
            }
        }
    }

    @Override
    public void onClose(Player who) {
        ContainerClosePacket containerClosePacket = new ContainerClosePacket();
        containerClosePacket.setId(who.getWindowId(this));
        who.sendPacket(containerClosePacket);
        super.onClose(who);

        EnderChest chest = who.getViewingEnderChest();
        if (chest != null && chest.getInventory().getViewers().size() == 1) {
            World world = this.getHolder().getWorld();
            if (world != null) {
                world.addLevelSoundEvent(this.getHolder().getPosition().add(0.5, 0.5, 0.5), SoundEvent.ENDERCHEST_CLOSED);
                ContainerInventory.sendBlockEventPacket(world.getBlockEntity(chest.getPosition()), 0);
            }

            who.setViewingEnderChest(null);
        }

        super.onClose(who);
    }
}
