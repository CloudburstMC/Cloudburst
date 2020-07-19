package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * @author Nukkit Project Team
 */
public class BlockBehaviorPressurePlateWood extends BlockBehaviorPressurePlateBase {

    public BlockBehaviorPressurePlateWood(Identifier id) {
        super(id);
        this.onPitch = 0.8f;
        this.offPitch = 0.7f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
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
    public Item[] getDrops(Item hand) {
        return new Item[]{
                toItem()
        };
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    @Override
    protected int computeRedstoneStrength() {
        AxisAlignedBB bb = getCollisionBoxes();

        for (Entity entity : this.level.getCollidingEntities(bb)) {
            if (entity.canTriggerPressurePlate()) {
                return 15;
            }
        }

        return 0;
    }
}