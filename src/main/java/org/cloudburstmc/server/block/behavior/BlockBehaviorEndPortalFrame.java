package org.cloudburstmc.server.block.behavior;

import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.item.ItemIds.ENDER_EYE;

public class BlockBehaviorEndPortalFrame extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 18000000;
    }

    @Override
    public float getHardness() {
        return -1;
    }

    @Override
    public int getLightLevel() {
        return 1;
    }

    @Override
    public boolean isBreakable(Item item) {
        return false;
    }

    @Override
    public float getMaxY() {
        return this.getY() + ((this.getMeta() & 0x04) > 0 ? 1 : 0.8125f);
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    public int getComparatorInputOverride() {
        return (getMeta() & 4) != 0 ? 15 : 0;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if ((this.getMeta() & 0x04) == 0 && player != null && item.getId() == ENDER_EYE) {
            this.setMeta(this.getMeta() + 4);
            this.getLevel().setBlock(this.getPosition(), this, true, true);
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.BLOCK_END_PORTAL_FRAME_FILL);
            //TODO: create portal
            return true;
        }
        return false;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, 0);
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x07);
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.GREEN_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
