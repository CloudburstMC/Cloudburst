package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
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
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return hand.getTier() >= ItemToolBehavior.TIER_WOODEN ? new ItemStack[]{this.toItem(block)} : new ItemStack[0];
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
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
