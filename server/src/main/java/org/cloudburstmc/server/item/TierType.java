package org.cloudburstmc.server.item;

public interface TierType extends Comparable<TierType> {

    int getLevel();

    float getMiningEfficiency();

    int getDurability();

    @Override
    default int compareTo(TierType that) {
        return Integer.compare(this.getLevel(), that.getLevel());
    }
}
