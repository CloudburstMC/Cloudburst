package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nullable;

public interface ItemType {

    Identifier getId();

    boolean isBlock();

    boolean isPlaceable();

    @Nullable
    BlockType getBlock();

    @Nullable
    Class<?> getMetadataClass();

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

    @Nullable
    ToolType getToolType();

    @Nullable
    TierType getTierType();
}
