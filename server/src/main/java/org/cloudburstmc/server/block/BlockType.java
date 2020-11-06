package org.cloudburstmc.server.block;

import org.cloudburstmc.server.item.ItemType;
import org.cloudburstmc.server.item.TierType;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.math.AxisAlignedBB;

public interface BlockType extends ItemType {

    @Override
    default boolean isBlock() {
        return true;
    }

    boolean isDiggable();

    boolean isTransparent();

    boolean isFlammable();

    int emitsLight();

    int getLightFilter();

    float getHardness();

    boolean isFloodable();

    boolean isSolid();

    int getBurnChance();

    int getBurnAbility();

    float getResistance();

    float getFriction();

    float getTranslucency();

    boolean pushesOutItems();

    boolean isFallable();

    boolean isExperimental();

    boolean isReplaceable();

    boolean isPowerSource();

    boolean breaksFalling();

    boolean blocksWater();

    boolean canBeSilkTouched();

    boolean blocksSolid();

    boolean blocksMotion();

    boolean hasComparatorSignal();

    boolean pushesUpFalling();

    boolean breaksFlowing();

    boolean waterlogsSource();

    ToolType getTargetToolType();

    TierType getToolTier();

    BlockState createBlockState();

    AxisAlignedBB getBoundingBox();

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
