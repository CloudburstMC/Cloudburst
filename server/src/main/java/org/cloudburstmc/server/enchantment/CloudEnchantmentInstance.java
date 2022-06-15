package org.cloudburstmc.server.enchantment;

import com.google.common.base.Preconditions;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentTarget;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.behavior.ItemBehavior;
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
        ItemBehavior behavior = item.getBehavior();
        ItemType type = item.getType();

        return switch (target) {
            case ARMOR -> behavior.isArmor();
            case ARMOR_CHEST -> behavior.isChestplate();
            case ARMOR_FEET -> behavior.isBoots();
            case ARMOR_HEAD -> behavior.isHelmet();
            case ARMOR_LEGS -> behavior.isLeggings();
            case BOW -> type == ItemTypes.BOW;
            case BREAKABLE -> behavior.getMaxDurability() > 0;
            case CROSSBOW -> type == ItemTypes.CROSSBOW;
            case FISHING_ROD -> type == ItemTypes.FISHING_ROD;
            case TOOL -> behavior.isAxe() || behavior.isHoe() || behavior.isPickaxe() || behavior.isShovel();
            case TRIDENT -> type == ItemTypes.TRIDENT;
            // TODO: VANISHABLE
            case WEAPON -> behavior.isSword();
            case WEARABLE -> behavior.isArmor() || type == ItemTypes.ELYTRA || type == ItemTypes.SKULL || type == BlockTypes.PUMPKIN;
            default -> false;
        };
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
