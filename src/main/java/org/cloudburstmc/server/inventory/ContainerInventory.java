package org.cloudburstmc.server.inventory;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.BlockEventPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Map;

import static org.cloudburstmc.api.block.BlockTypes.AIR;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ContainerInventory extends BaseInventory {
    public ContainerInventory(InventoryHolder holder, InventoryType type) {
        super(holder, type);
    }

    public ContainerInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items) {
        super(holder, type, items);
    }

    public ContainerInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items, Integer overrideSize) {
        super(holder, type, items, overrideSize);
    }

    public ContainerInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items, Integer overrideSize, String overrideTitle) {
        super(holder, type, items, overrideSize, overrideTitle);
    }

    @Override
    public void onOpen(CloudPlayer who) {
        super.onOpen(who);
        ContainerOpenPacket packet = new ContainerOpenPacket();
        packet.setId(who.getWindowId(this));
        packet.setType(ContainerType.from(this.getType().getNetworkType()));
        InventoryHolder holder = this.getHolder();
        if (holder instanceof BlockEntity) {
            packet.setBlockPosition(((BlockEntity) holder).getPosition());
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

    public static int calculateRedstone(Inventory inv) {
        if (inv == null) {
            return 0;
        } else {
            int itemCount = 0;
            float averageCount = 0;

            for (int slot = 0; slot < inv.getSize(); ++slot) {
                ItemStack item = inv.getItem(slot);

                if (item.getType() != AIR) {
                    averageCount += (float) item.getAmount() / (float) Math.min(inv.getMaxStackSize(), item.getBehavior().getMaxStackSize(item));
                    ++itemCount;
                }
            }

            averageCount = averageCount / (float) inv.getSize();
            return NukkitMath.floorFloat(averageCount * 14) + (itemCount > 0 ? 1 : 0);
        }
    }

    public static void sendBlockEventPacket(BlockEntity block, int eventData) {
        if (block.getLevel() == null) return;
        BlockEventPacket bep = new BlockEventPacket();
        bep.setBlockPosition(block.getPosition());
        bep.setEventType(1);
        bep.setEventData(eventData);
        block.getLevel().addChunkPacket(block.getPosition(), bep);
    }
}
