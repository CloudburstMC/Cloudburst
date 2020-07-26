package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorWeightedPressurePlateHeavy extends BlockBehaviorPressurePlateBase {

    public BlockBehaviorWeightedPressurePlateHeavy() {
        this.onPitch = 0.90000004f;
        this.offPitch = 0.75f;
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
        return ItemTool.TYPE_PICKAXE;
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
        return Item.get(id, 0);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.IRON_BLOCK_COLOR;
    }

    @Override
    protected int computeRedstoneStrength() {
        int count = Math.min(this.level.getCollidingEntities(getCollisionBoxes()).size(), this.getMaxWeight());

        if (count > 0) {
            float f = (float) Math.min(this.getMaxWeight(), count) / (float) this.getMaxWeight();
            return Math.max(1, NukkitMath.ceilFloat(f * 15.0F));
        } else {
            return 0;
        }
    }

    public int getMaxWeight() {
        return 150;
    }
}
