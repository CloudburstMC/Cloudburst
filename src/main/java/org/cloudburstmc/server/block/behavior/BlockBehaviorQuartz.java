package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.QUARTZ_BLOCK;

public class BlockBehaviorQuartz extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0.8f;
    }

    @Override
    public float getResistance() {
        return 4;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (this.getMeta() != QUARTZ_NORMAL) {
            short[] faces = new short[]{
                    0,
                    0,
                    0b1000,
                    0b1000,
                    0b0100,
                    0b0100
            };

            this.setMeta(((this.getMeta() & 0x03) | faces[face.getIndex()]));
        }
        this.getLevel().setBlock(block.getPosition(), this, true, true);

        return true;
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
    public Item toItem(Block block) {
        return Item.get(QUARTZ_BLOCK, this.getMeta() & 0x03, 1);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.QUARTZ_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
