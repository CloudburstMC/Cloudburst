package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.EnchantingTable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.inventory.EnchantInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.network.protocol.types.ContainerIds;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.api.blockentity.BlockEntityTypes.ENCHANTING_TABLE;

public class BlockBehaviorEnchantingTable extends BlockBehaviorTransparent {


    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        placeBlock(block, item);

        EnchantingTable enchantingTable = BlockEntityRegistry.get().newEntity(ENCHANTING_TABLE, block);
        enchantingTable.loadAdditionalData(((CloudItemStack) item).getDataTag());
        if (item.hasName()) {
            enchantingTable.setCustomName(item.getName());
        }
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        if (player != null) {
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
            if (!(blockEntity instanceof EnchantingTable)) {
                BlockEntityRegistry.get().newEntity(ENCHANTING_TABLE, block);
            }

            player.addWindow(new EnchantInventory(player.getUIInventory(), block), ContainerIds.ENCHANTING_TABLE);
        }

        return true;
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.RED_BLOCK_COLOR;
    }


}
