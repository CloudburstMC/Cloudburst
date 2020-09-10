package org.cloudburstmc.server.enchantment;

/**
 * @author Nukkit Project Team
 */
public class EnchantmentEntry {

    private final EnchantmentInstance[] enchantments;
    private final int cost;
    private final String randomName;

    public EnchantmentEntry(EnchantmentInstance[] enchantments, int cost, String randomName) {
        this.enchantments = enchantments;
        this.cost = cost;
        this.randomName = randomName;
    }

    public EnchantmentInstance[] getEnchantments() {
        return enchantments;
    }

    public int getCost() {
        return cost;
    }

    public String getRandomName() {
        return randomName;
    }

}
