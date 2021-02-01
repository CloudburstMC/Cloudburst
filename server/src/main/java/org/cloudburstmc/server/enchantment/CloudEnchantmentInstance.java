package org.cloudburstmc.server.enchantment;

import com.google.common.base.Preconditions;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.server.item.ItemType;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.behavior.ItemArmorBehavior;
import org.cloudburstmc.server.item.behavior.ItemBehavior;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CloudEnchantmentInstance implements EnchantmentInstance {

    private final EnchantmentType type;
    private final int level;

    public CloudEnchantmentInstance(EnchantmentType type, int level) {
        Preconditions.checkNotNull(type);
        this.type = type;
        this.level = level;
    }

    @Override
    public EnchantmentType getType() {
        return type;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public EnchantmentBehavior getBehavior() {
        return EnchantmentRegistry.get().getBehavior(this.type);
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        EnchantmentTarget target = this.getType().getTarget();
        if(target == EnchantmentTarget.ALL) {
            return true;
        }

        ItemBehavior behavior = item.getBehavior();
        if(target == EnchantmentTarget.BREAKABLE && behavior.getMaxDurability() >= 0) {
            return true;
        }

        if (behavior instanceof ItemArmorBehavior) { //TODO: Fix
            if (target == EnchantmentTarget.ARMOR) {
                return true;
            }

            switch (target) {
                case ARMOR_HEAD:
                    return behavior.isHelmet();
                case ARMOR_TORSO:
                    return behavior.isChestplate();
                case ARMOR_LEGS:
                    return behavior.isLeggings();
                case ARMOR_FEET:
                    return behavior.isBoots();
                default:
                    return false;
            }
        }

        ItemType type = item.getType();

        switch (target) {
            case SWORD:
                return behavior.isSword();
            case DIGGER:
                return behavior.isPickaxe() || behavior.isShovel() || behavior.isAxe();
            case BOW:
                return type == ItemTypes.BOW;
            case FISHING_ROD:
                return type == ItemTypes.FISHING_ROD;
            case WEARABLE:
                return behavior.isArmor() || type == ItemTypes.ELYTRA || type == ItemTypes.SKULL || type == BlockTypes.PUMPKIN;
            case TRIDENT:
                return type == ItemTypes.TRIDENT;
            default:
                return false;
        }
    }

    public static final String[] words = {"the", "elder", "scrolls", "klaatu", "berata", "niktu", "xyzzy", "bless", "curse", "light", "darkness", "fire", "air", "earth", "water", "hot", "dry", "cold", "wet", "ignite", "snuff", "embiggen", "twist", "shorten", "stretch", "fiddle", "destroy", "imbue", "galvanize", "enchant", "free", "limited", "range", "of", "towards", "inside", "sphere", "cube", "self", "other", "ball", "mental", "physical", "grow", "shrink", "demon", "elemental", "spirit", "animal", "creature", "beast", "humanoid", "undead", "fresh", "stale"};

    public static String getRandomName() {
        int count = ThreadLocalRandom.current().nextInt(3, 6);
        HashSet<String> set = new HashSet<>();
        while (set.size() < count) {
            set.add(CloudEnchantmentInstance.words[ThreadLocalRandom.current().nextInt(0, CloudEnchantmentInstance.words.length)]);
        }

        String[] words = set.toArray(new String[0]);
        return String.join(" ", words);
    }
}
