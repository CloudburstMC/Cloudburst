package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.Barrel;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.ItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockIds.BARREL;

public class BlockBehaviorBarrel extends BlockBehaviorSolid {

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState newState = BlockState.get(BARREL);
        Direction facing;

        if (Math.abs(player.getX() - block.getX()) < 2 && Math.abs(player.getZ() - block.getZ()) < 2) {
            float y = player.getY() + player.getEyeHeight();

            if (y - block.getY() > 2) {
                facing = Direction.UP;
            } else if (block.getY() - y > 0) {
                facing = Direction.DOWN;
            } else {
                facing = player.getHorizontalDirection().getOpposite();
            }
        } else {
            facing = player.getHorizontalDirection().getOpposite();
        }

        newState = newState.withTrait(BlockTraits.FACING_DIRECTION, facing);
        block.set(newState, true, false);

        Barrel barrel = BlockEntityRegistry.get().newEntity(BlockEntityTypes.BARREL, block.getChunk(), block.getPosition());
        barrel.loadAdditionalData(item.getTag());

        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player == null) {
            return false;
        }

        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());
        Barrel barrel;
        if (blockEntity instanceof Barrel) {
            barrel = (Barrel) blockEntity;
        } else {
            barrel = BlockEntityRegistry.get().newEntity(BlockEntityTypes.BARREL, block.getChunk(), block.getPosition());
        }

        player.addWindow(barrel.getInventory());

        return true;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public float getHardness() {
        return 2.5f;
    }

    @Override
    public float getResistance() {
        return 12.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    @Override
    public Item toItem(Block block) {
        return ItemRegistry.get().getItem(block.getState().defaultState());
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Barrel) {
            return ContainerInventory.calculateRedstone(((Barrel) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }
}