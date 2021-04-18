package org.cloudburstmc.server.item.randomitem;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.data.DyeColor;

/**
 * Created by Snake1999 on 2016/1/15.
 * Package cn.nukkit.item.randomitem in project nukkit.
 */
public final class Fishing {

    public static final Selector ROOT_FISHING = RandomItem.putSelector(new Selector(RandomItem.ROOT));

    public static final Selector FISHES = RandomItem.putSelector(new Selector(ROOT_FISHING), 0.85F);
    public static final Selector TREASURES = RandomItem.putSelector(new Selector(ROOT_FISHING), 0.05F);
    public static final Selector JUNKS = RandomItem.putSelector(new Selector(ROOT_FISHING), 0.1F);
    public static final Selector FISH = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.FISH, FISHES), 0.6F);
    public static final Selector SALMON = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.SALMON, FISHES), 0.25F);
    public static final Selector CLOWNFISH = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.CLOWNFISH, FISHES), 0.02F);
    public static final Selector PUFFERFISH = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.PUFFERFISH, FISHES), 0.13F);
    public static final Selector TREASURE_BOW = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.BOW, TREASURES), 0.1667F);
    public static final Selector TREASURE_ENCHANTED_BOOK = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.ENCHANTED_BOOK, TREASURES), 0.1667F);
    public static final Selector TREASURE_FISHING_ROD = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.FISHING_ROD, TREASURES), 0.1667F);
    public static final Selector TREASURE_NAME_TAG = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.NAME_TAG, TREASURES), 0.1667F);
    public static final Selector TREASURE_SADDLE = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.SADDLE, TREASURES), 0.1667F);
    public static final Selector JUNK_BOWL = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.BOWL, JUNKS), 0.12F);
    public static final Selector JUNK_FISHING_ROD = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.FISHING_ROD, JUNKS), 0.024F);
    public static final Selector JUNK_LEATHER = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.LEATHER, JUNKS), 0.12F);
    public static final Selector JUNK_LEATHER_BOOTS = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.LEATHER_BOOTS, JUNKS), 0.12F);
    public static final Selector JUNK_ROTTEN_FLESH = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.ROTTEN_FLESH, JUNKS), 0.12F);
    public static final Selector JUNK_STICK = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.STICK, JUNKS), 0.06F);
    public static final Selector JUNK_STRING_ITEM = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.STRING, JUNKS), 0.06F);
    public static final Selector JUNK_WATTER_BOTTLE = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.POTION, 0, JUNKS), 0.12F);
    public static final Selector JUNK_BONE = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.BONE, JUNKS), 0.12F);
    public static final Selector JUNK_INK_SAC = RandomItem.putSelector(new ConstantItemSelector(ItemTypes.DYE, 10, JUNKS, DyeColor.BLACK.getDyeData()), 0.012F);
    public static final Selector JUNK_TRIPWIRE_HOOK = RandomItem.putSelector(new ConstantItemSelector(BlockTypes.TRIPWIRE_HOOK, JUNKS), 0.12F);

    public static ItemStack getFishingResult(ItemStack rod) {
        int fortuneLevel = 0;
        int lureLevel = 0;
        if (rod != null) {
            if (rod.getEnchantment(EnchantmentTypes.FORTUNE) != null) {
                fortuneLevel = rod.getEnchantment(EnchantmentTypes.FORTUNE).getLevel();
            } else if (rod.getEnchantment(EnchantmentTypes.LURE) != null) {
                lureLevel = rod.getEnchantment(EnchantmentTypes.LURE).getLevel();
            }
        }
        return getFishingResult(fortuneLevel, lureLevel);
    }

    public static ItemStack getFishingResult(int fortuneLevel, int lureLevel) {
        float treasureChance = limitRange(0, 1, 0.05f + 0.01f * fortuneLevel - 0.01f * lureLevel);
        float junkChance = limitRange(0, 1, 0.05f - 0.025f * fortuneLevel - 0.01f * lureLevel);
        float fishChance = limitRange(0, 1, 1 - treasureChance - junkChance);
        RandomItem.putSelector(FISHES, fishChance);
        RandomItem.putSelector(TREASURES, treasureChance);
        RandomItem.putSelector(JUNKS, junkChance);
        Object result = RandomItem.selectFrom(ROOT_FISHING);
        if (result instanceof ItemStack) return (ItemStack) result;
        return null;
    }

    private static float limitRange(float min, float max, float value) {
        if (value >= max) return max;
        if (value <= min) return min;
        return value;
    }
}
