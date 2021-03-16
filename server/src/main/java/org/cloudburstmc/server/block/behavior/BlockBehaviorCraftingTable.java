package org.cloudburstmc.server.block.behavior;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.CloudPlayer.CraftingType;

public class BlockBehaviorCraftingTable extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }


    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        if (player != null) {
            player.craftingType = CraftingType.BIG;
            player.setCraftingGrid(player.getUIInventory().getBigCraftingGrid());
            ContainerOpenPacket pk = new ContainerOpenPacket();
            pk.setId((byte) -1);
            pk.setBlockPosition(block.getPosition());
            pk.setType(ContainerType.WORKBENCH);
            pk.setUniqueEntityId(player.getUniqueId());
            player.sendPacket(pk);
        }
        return true;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }
}
