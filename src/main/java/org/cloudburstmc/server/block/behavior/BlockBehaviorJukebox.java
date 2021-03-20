package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.Jukebox;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.inventory.PlayerInventory;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.AIR;
import static org.cloudburstmc.api.blockentity.BlockEntityTypes.JUKEBOX;

public class BlockBehaviorJukebox extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().getType());
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        if (!(blockEntity instanceof Jukebox)) {
            blockEntity = this.createBlockEntity(block);
        }

        Jukebox jukebox = (Jukebox) blockEntity;
        if (jukebox.getRecordItem().getType() != AIR) {
            jukebox.dropItem();
        } else if (item.getType() == ItemTypes.RECORD) {
            jukebox.setRecordItem(item);
            jukebox.play();
            player.getInventory().decrementCount(((PlayerInventory) player.getInventory()).getHeldItemIndex());
        }

        return false;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (placeBlock(block, item)) {
            createBlockEntity(block);
            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        if (super.onBreak(block, item)) {
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

            if (blockEntity instanceof Jukebox) {
                ((Jukebox) blockEntity).dropItem();
            }
            return true;
        }

        return false;
    }

    private BlockEntity createBlockEntity(Block block) {
        return BlockEntityRegistry.get().newEntity(JUKEBOX, block);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}
