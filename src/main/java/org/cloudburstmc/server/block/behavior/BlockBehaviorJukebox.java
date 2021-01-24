package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Jukebox;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.RecordItem;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockIds.AIR;
import static org.cloudburstmc.server.blockentity.BlockEntityTypes.JUKEBOX;

public class BlockBehaviorJukebox extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(block.getState().getType());
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());
        if (!(blockEntity instanceof Jukebox)) {
            blockEntity = this.createBlockEntity(block);
        }

        Jukebox jukebox = (Jukebox) blockEntity;
        if (jukebox.getRecordItem().getId() != AIR) {
            jukebox.dropItem();
        } else if (item instanceof RecordItem) {
            jukebox.setRecordItem(item);
            jukebox.play();
            player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
        }

        return false;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (placeBlock(block, item)) {
            createBlockEntity(block);
            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        if (super.onBreak(block, item)) {
            BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());

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
