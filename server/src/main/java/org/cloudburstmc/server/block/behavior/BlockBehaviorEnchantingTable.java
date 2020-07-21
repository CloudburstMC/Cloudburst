package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.EnchantingTable;
import org.cloudburstmc.server.inventory.EnchantInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.network.protocol.types.ContainerIds;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.ENCHANTING_TABLE;

public class BlockBehaviorEnchantingTable extends BlockBehaviorTransparent {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 5;
    }

    @Override
    public float getResistance() {
        return 6000;
    }

    @Override
    public int getLightLevel() {
        return 12;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem(blockState)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        this.getLevel().setBlock(block.getPosition(), this, true, true);

        EnchantingTable enchantingTable = BlockEntityRegistry.get().newEntity(ENCHANTING_TABLE, this.getChunk(), this.getPosition());
        enchantingTable.loadAdditionalData(item.getTag());
        if (item.hasCustomName()) {
            enchantingTable.setCustomName(item.getCustomName());
        }
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null) {
            BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition());
            if (!(blockEntity instanceof EnchantingTable)) {
                BlockEntityRegistry.get().newEntity(ENCHANTING_TABLE, this.getChunk(), this.getPosition());
            }

            player.addWindow(new EnchantInventory(player.getUIInventory(), this), ContainerIds.ENCHANTING_TABLE);
        }

        return true;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.RED_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
