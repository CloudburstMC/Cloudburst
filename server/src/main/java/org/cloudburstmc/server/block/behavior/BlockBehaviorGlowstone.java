package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.Random;

public class BlockBehaviorGlowstone extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 1.5f;
    }

    @Override
    public float getHardness() {
        return 0.3f;
    }

    @Override
    public int getLightLevel(Block block) {
        return 15;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        Random random = new Random();
        int count = 2 + random.nextInt(3);

        Enchantment fortune = hand.getEnchantment(Enchantment.ID_FORTUNE_DIGGING);
        if (fortune != null && fortune.getLevel() >= 1) {
            count += random.nextInt(fortune.getLevel() + 1);
        }

        return new Item[]{
                Item.get(ItemIds.GLOWSTONE_DUST, 0, MathHelper.clamp(count, 1, 4))
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
