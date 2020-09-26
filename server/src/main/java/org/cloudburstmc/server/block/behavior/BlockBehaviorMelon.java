package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.Random;

public class BlockBehaviorMelon extends BlockBehaviorSolid {

    public float getHardness() {
        return 1;
    }

    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        Random random = new Random();
        int count = 3 + random.nextInt(5);

        EnchantmentInstance fortune = hand.getEnchantment(CloudEnchantmentInstance.ID_FORTUNE_DIGGING);
        if (fortune != null && fortune.getLevel() >= 1) {
            count += random.nextInt(fortune.getLevel() + 1);
        }

        return new ItemStack[]{
                ItemStack.get(ItemTypes.MELON, 0, Math.min(9, count))
        };
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_AXE;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.LIME_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
