package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorPumpkin extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 1;
    }

    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, 0);
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        this.setMeta(player != null ? player.getDirection().getOpposite().getHorizontalIndex() : 0);
        this.getLevel().setBlock(block.getPosition(), this, true, true);
        return true;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.ORANGE_BLOCK_COLOR;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x7);
    }
}
