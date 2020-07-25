package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorRedstoneLamp extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0.3f;
    }

    @Override
    public float getResistance() {
        return 1.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        if (this.level.isBlockPowered(this.getPosition())) {
            this.level.setBlock(this.getPosition(), BlockState.get(BlockTypes.LIT_REDSTONE_LAMP), false, true);
        } else {
            this.level.setBlock(this.getPosition(), this, false, true);
        }
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) {
            // Redstone event
            RedstoneUpdateEvent ev = new RedstoneUpdateEvent(this);
            getLevel().getServer().getPluginManager().callEvent(ev);
            if (ev.isCancelled()) {
                return 0;
            }
            if (this.level.isBlockPowered(this.getPosition())) {
                this.level.setBlock(this.getPosition(), BlockState.get(BlockTypes.LIT_REDSTONE_LAMP), false, false);
                return 1;
            }
        }

        return 0;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                Item.get(BlockTypes.REDSTONE_LAMP)
        };
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
