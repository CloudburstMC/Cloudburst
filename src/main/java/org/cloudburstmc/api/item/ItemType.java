package org.cloudburstmc.api.item;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.util.Identifier;

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

    default ItemStack createItem() {
        return createItem(1);
    }

    ItemStack createItem(int amount, Object... metadata);

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

    default short getFuelTime() {
        return 0;
    }

    default BlockType getBlockType() {
        return null;
    }

    default boolean isStackable() {
        return getMaximumStackSize() > 1;
    }

    @Nullable
    ToolType getToolType();

    @Nullable
    TierType getTierType();
}
