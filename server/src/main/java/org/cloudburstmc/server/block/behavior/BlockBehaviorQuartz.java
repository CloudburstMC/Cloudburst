package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Axis;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

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
        return placeBlock(block, item.getBlock().withTrait(
                BlockTraits.AXIS,
                player != null ? player.getDirection().getAxis() : Axis.Y
        ));
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
        return Item.get(block.getState().resetTrait(BlockTraits.AXIS));
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
