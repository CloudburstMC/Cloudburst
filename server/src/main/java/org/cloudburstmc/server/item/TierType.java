package org.cloudburstmc.server.item;

public interface TierType extends Comparable<TierType> {

    int getLevel();

    float getMiningEfficiency();

    int getDurability();

    TierTarget getTarget();

    int getToolEnchantAbility();

    int getArmorEnchantAbility();

    @Override
    default int compareTo(TierType that) {
        return Integer.compare(this.getLevel(), that.getLevel());
    }

    enum TierTarget {
        TOOL,
        ARMOR,
        ANY
    }
}
