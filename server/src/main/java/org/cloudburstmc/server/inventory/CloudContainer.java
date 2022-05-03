package org.cloudburstmc.server.inventory;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.BlockEventPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.inventory.ContainerInventory;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Map;

import static org.cloudburstmc.api.block.BlockTypes.AIR;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class CloudContainer extends BaseInventory implements ContainerInventory {
    public CloudContainer(InventoryHolder holder, InventoryType type) {
        super(holder, type);
    }

    public CloudContainer(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items) {
        super(holder, type, items);
    }

    public CloudContainer(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items, Integer overrideSize) {
        super(holder, type, items, overrideSize);
    }

    public CloudContainer(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items, Integer overrideSize, String overrideTitle) {
        super(holder, type, items, overrideSize, overrideTitle);
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);
        ContainerOpenPacket packet = new ContainerOpenPacket();
        packet.setId(who.getWindowId(this));
        packet.setType(NetworkUtils.inventoryToNetwork(this.getType()));
        InventoryHolder holder = this.getHolder();
        if (holder instanceof BlockEntity) {
            packet.setBlockPosition(((BlockEntity) holder).getPosition());
        } else {
            packet.setBlockPosition(Vector3i.ZERO);
        }
        ((CloudPlayer) who).sendPacket(packet);

        this.sendContents(who);
    }

    @Override
    public void onClose(Player who) {
        ContainerClosePacket packet = new ContainerClosePacket();
        packet.setId(who.getWindowId(this));
        ((CloudPlayer) who).sendPacket(packet);
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
                    int maxStackSize = CloudItemRegistry.get().getBehavior(item.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute();
                    averageCount += (float) item.getCount() / (float) Math.min(inv.getMaxStackSize(), maxStackSize);
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
        ((CloudLevel) block.getLevel()).addChunkPacket(block.getPosition(), bep);
    }
}
