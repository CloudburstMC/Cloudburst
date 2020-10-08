package org.cloudburstmc.server.block.behavior;


import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BrewingStand;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.TierTypes;
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
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState state = block.getState();
        if (!state.inCategory(BlockCategory.TRANSPARENT)) {
            placeBlock(block, BlockRegistry.get().getBlock(BlockTypes.BREWING_STAND));

            BrewingStand brewingStand = BlockEntityRegistry.get().newEntity(BREWING_STAND, block.getChunk(), block.getPosition());
            brewingStand.loadAdditionalData(((CloudItemStack) item).getDataTag());
            if (item.hasName()) {
                brewingStand.setCustomName(item.getName());
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
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
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.BREWING_STAND);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getBehavior().isPickaxe() && hand.getBehavior().getTier(hand).compareTo(TierTypes.WOOD) >= 0) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.IRON_BLOCK_COLOR;
    }


    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof BrewingStand) {
            return ContainerInventory.calculateRedstone(((BrewingStand) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }


}
