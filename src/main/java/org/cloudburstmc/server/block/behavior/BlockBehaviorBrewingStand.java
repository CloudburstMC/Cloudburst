package org.cloudburstmc.server.block.behavior;


import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BrewingStand;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.BREWING_STAND;

public class BlockBehaviorBrewingStand extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public int getLightLevel(Block block) {
        return 1;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState state = block.getState();
        if (!state.inCategory(BlockCategory.TRANSPARENT)) {
            placeBlock(block, BlockRegistry.get().getBlock(BlockIds.BREWING_STAND));

            BrewingStand brewingStand = BlockEntityRegistry.get().newEntity(BREWING_STAND, block.getChunk(), block.getPosition());
            brewingStand.loadAdditionalData(item.getTag());
            if (item.hasCustomName()) {
                brewingStand.setCustomName(item.getCustomName());
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null) {
            BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());
            BrewingStand brewing;
            if (blockEntity instanceof BrewingStand) {
                brewing = (BrewingStand) blockEntity;
            } else {
                if (blockEntity != null) {
                    blockEntity.close();
                }

                brewing = BlockEntityRegistry.get().newEntity(BREWING_STAND, block.getChunk(), block.getPosition());
            }

            player.addWindow(brewing.getInventory());
        }

        return true;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.BREWING_STAND);
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem(block)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.IRON_BLOCK_COLOR;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());

        if (blockEntity instanceof BrewingStand) {
            return ContainerInventory.calculateRedstone(((BrewingStand) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
