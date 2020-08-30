package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorTerracottaGlazed extends BlockBehaviorSolid {

    @Override
    public float getResistance() {
        return 7;
    }

    @Override
    public float getHardness() {
        return 1.4f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return hand.getTier() >= ItemTool.TIER_WOODEN ? new Item[]{this.toItem(block)} : new Item[0];
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item.getBlock()
                .withTrait(
                        BlockTraits.FACING_DIRECTION,
                        player != null ? player.getHorizontalDirection().getOpposite() : Direction.NORTH
                ));
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
