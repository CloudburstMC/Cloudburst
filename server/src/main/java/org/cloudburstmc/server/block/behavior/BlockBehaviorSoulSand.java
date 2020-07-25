package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Pub4Game on 27.12.2015.
 */
public class BlockBehaviorSoulSand extends BlockBehaviorSolid {

    public BlockBehaviorSoulSand(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 1 - 0.125f;
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public void onEntityCollide(Entity entity) {
        entity.setMotion(entity.getMotion().mul(0.4, 1, 0.4));
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.BROWN_BLOCK_COLOR;
    }

}
