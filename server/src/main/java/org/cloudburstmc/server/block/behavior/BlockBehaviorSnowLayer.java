package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public class BlockBehaviorSnowLayer extends BlockBehaviorFallable {

    @Override
    public float getHardness() {
        return 0.1f;
    }

    @Override
    public float getResistance() {
        return 0.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public boolean canBeReplaced() {
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = this.down();
        if (down.isSolid()) {
            this.getLevel().setBlock(blockState.getPosition(), this, true);

            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        super.onUpdate(block, type);
        if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (this.getLevel().getBlockLightAt((int) this.getX(), (int) this.getY(), (int) this.getZ()) >= 10) {
                BlockFadeEvent event = new BlockFadeEvent(this, BlockState.get(AIR));
                level.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    level.setBlock(this.getPosition(), event.getNewState(), true);
                }
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(ItemIds.SNOWBALL);
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isShovel() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    this.toItem(blockState)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.SNOW_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean canBeFlooded() {
        return true;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {
        return null;
    }
}


