package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by CreeperFace on 10.4.2017.
 */
public class BlockBehaviorRedstoneRepeaterPowered extends BlockBehaviorRedstoneDiode {

    public BlockBehaviorRedstoneRepeaterPowered(Identifier id) {
        super(id);
        this.isPowered = true;
    }

    @Override
    public BlockFace getFacing() {
        return BlockFace.fromHorizontalIndex(getMeta());
    }

    @Override
    protected boolean isAlternateInput(BlockState blockState) {
        return isDiode(blockState);
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(ItemIds.REPEATER);
    }

    @Override
    protected int getDelay() {
        return (1 + (getMeta() >> 2)) * 2;
    }

    @Override
    protected BlockState getPowered() {
        return this;
    }

    @Override
    protected BlockState getUnpowered() {
        return BlockState.get(BlockTypes.UNPOWERED_REPEATER, this.getMeta());
    }

    @Override
    public int getLightLevel() {
        return 7;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        this.setMeta(this.getMeta() + 4);
        if (this.getMeta() > 15) this.setMeta(this.getMeta() % 4);

        this.level.setBlock(this.getPosition(), this, true, false);
        return true;
    }

    @Override
    public boolean isLocked() {
        return this.getPowerOnSides() > 0;
    }
}
