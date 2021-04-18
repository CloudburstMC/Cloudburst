package org.cloudburstmc.server.block.behavior;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.inventory.CraftingGrid;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.player.CloudPlayer;

public class BlockBehaviorCraftingTable extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }


    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            CloudPlayer p = (CloudPlayer) player;

            p.getCraftingInventory().setCraftingGridType(CraftingGrid.Type.CRAFTING_GRID_BIG);
            ContainerOpenPacket pk = new ContainerOpenPacket();
            pk.setId((byte) -1);
            pk.setBlockPosition(block.getPosition());
            pk.setType(ContainerType.WORKBENCH);
            pk.setUniqueEntityId(p.getUniqueId());
            p.sendPacket(pk);
        }
        return true;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }
}
