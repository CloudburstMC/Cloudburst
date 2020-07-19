package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.POWERED_REPEATER;

/**
 * Created by CreeperFace on 10.4.2017.
 */
public class BlockBehaviorRedstoneRepeaterUnpowered extends BlockBehaviorRedstoneDiode {

    public BlockBehaviorRedstoneRepeaterUnpowered(Identifier id) {
        super(id);
        this.isPowered = false;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        this.setMeta(this.getMeta() + 4);
        if (this.getMeta() > 15) this.setMeta(this.getMeta() % 4);

        this.level.setBlock(this.getPosition(), this, true, false);
        return true;
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
    public Item toItem() {
        return Item.get(ItemIds.REPEATER);
    }

    @Override
    protected int getDelay() {
        return (1 + (getMeta() >> 2)) * 2;
    }

    @Override
    protected BlockState getPowered() {
        return BlockState.get(POWERED_REPEATER, this.getMeta());
    }

    @Override
    protected BlockState getUnpowered() {
        return this;
    }

    @Override
    public boolean isLocked() {
        return this.getPowerOnSides() > 0;
    }
}