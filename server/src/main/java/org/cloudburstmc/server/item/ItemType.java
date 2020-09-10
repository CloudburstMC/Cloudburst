package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

import java.util.Optional;

public interface ItemType {

    Identifier getId();

    boolean isBlock();

    int getMaximumStackSize();

    default int getAttackDamage() {
        return 2;
    }

    default int getArmorPoints() {
        return 0;
    }

    default int getToughness() {
        return 0;
    }

    default int getDurability() {
        return 0;
    }

    default int getFuelTime() {
        return 0;
    }

    default boolean isStackable() {
        return getMaximumStackSize() > 1;
    }

    Optional<ToolType> getToolType();

    Optional<TierType> getTierType();
}
