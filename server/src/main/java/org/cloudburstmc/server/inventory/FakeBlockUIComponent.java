package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.event.inventory.InventoryOpenEvent;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.player.CloudPlayer;

public class FakeBlockUIComponent extends PlayerUIComponent {
    private final InventoryType type;

    FakeBlockUIComponent(PlayerUIInventory playerUI, InventoryType type, int offset, Block block) {
        super(playerUI, offset, type.getDefaultSize());
        this.type = type;
        this.holder = new FakeBlockMenu(this, block);
    }


    @Override
    public FakeBlockMenu getHolder() {
        return (FakeBlockMenu) this.holder;
    }

    @Override
    public void close(CloudPlayer who) {
        InventoryCloseEvent ev = new InventoryCloseEvent(this, who);
        who.getServer().getEventManager().fire(ev);

        this.onClose(who);
    }

    @Override
    public boolean open(CloudPlayer who) {
        InventoryOpenEvent ev = new InventoryOpenEvent(this, who);
        who.getServer().getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return false;
        }
        this.onOpen(who);

        return true;
    }

    @Override
    public void onOpen(CloudPlayer who) {
        super.onOpen(who);
        ContainerOpenPacket packet = new ContainerOpenPacket();
        packet.setId(who.getWindowId(this));
        packet.setType(ContainerType.from(type.getNetworkType()));
        InventoryHolder holder = this.getHolder();
        if (holder != null) {
            packet.setBlockPosition(((FakeBlockMenu) holder).getPosition());
        } else {
            packet.setBlockPosition(Vector3i.ZERO);
        }

        who.sendPacket(packet);

        this.sendContents(who);
    }

    @Override
    public void onClose(CloudPlayer who) {
        ContainerClosePacket packet = new ContainerClosePacket();
        packet.setId(who.getWindowId(this));
        who.sendPacket(packet);
        super.onClose(who);
    }
}
