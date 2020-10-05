package org.cloudburstmc.server.block;

import org.cloudburstmc.server.item.ItemType;
import org.cloudburstmc.server.item.TierType;
import org.cloudburstmc.server.item.ToolType;

public interface BlockType extends ItemType {

    @Override
    default boolean isBlock() {
        return true;
    }

    boolean isDiggable();

    boolean isTransparent();

    boolean isFlammable();

    int emitsLight();

    int filtersLight();

    float getHardness();

    boolean isFloodable();

    boolean isSolid();

    int getBurnChance();

    int getBurnAbility();

    float getResistance();

    float getFriction();

    boolean pushesOutItems();

    boolean isFallable();

    boolean isExperimental();

    boolean isReplaceable();

    ToolType getTargetToolType();

    TierType getToolTier();

    @Override
    default boolean isPlaceable() {
        return false;
    }

    @Override
    default BlockType getBlock() {
        return this;
    }

    @Override
    default ToolType getToolType() {
        return null;
    }

    @Override
    default TierType getTierType() {
        return null;
    }
}
