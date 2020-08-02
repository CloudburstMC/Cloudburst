package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

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
    public boolean canBeReplaced(Block block) {
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = block.down().getState();
        if (down.inCategory(BlockCategory.SOLID)) {
            placeBlock(block, BlockState.get(BlockTypes.SNOW_LAYER));
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        super.onUpdate(block, type);
        if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (block.getLevel().getBlockLightAt(block.getX(), block.getY(), block.getZ()) >= 10) {
                BlockFadeEvent event = new BlockFadeEvent(block, BlockState.AIR);
                block.getLevel().getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    block.set(event.getNewState());
                }
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.SNOWBALL);
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isShovel() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    this.toItem(block)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
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

//    @Override //TODO: bounding box
//    protected AxisAlignedBB recalculateBoundingBox() {
//        return null;
//    }
}


