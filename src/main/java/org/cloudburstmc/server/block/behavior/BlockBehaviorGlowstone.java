package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.Random;

public class BlockBehaviorGlowstone extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 1.5f;
    }


    @Override
    public int getLightLevel(Block block) {
        return 15;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        Random random = new Random();
        int count = 2 + random.nextInt(3);

        EnchantmentInstance fortune = hand.getEnchantment(CloudEnchantmentInstance.ID_FORTUNE_DIGGING);
        if (fortune != null && fortune.getLevel() >= 1) {
            count += random.nextInt(fortune.getLevel() + 1);
        }

        return new ItemStack[]{
                ItemStack.get(ItemTypes.GLOWSTONE_DUST, 0, MathHelper.clamp(count, 1, 4))
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.SAND_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
