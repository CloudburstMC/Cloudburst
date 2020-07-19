package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemChestplateLeather extends ItemColorArmor {

    public ItemChestplateLeather(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return TIER_LEATHER;
    }

    @Override
    public boolean isChestplate() {
        return true;
    }

    @Override
    public int getArmorPoints() {
        return 3;
    }

    @Override
    public int getMaxDurability() {
        return 81;
    }
}
